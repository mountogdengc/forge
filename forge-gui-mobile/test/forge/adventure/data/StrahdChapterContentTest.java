package forge.adventure.data;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import forge.adventure.util.AdventureQuestController;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import forge.util.Localizer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import static org.testng.Assert.*;

/** Tests the shipped dialogue branches and persistent quest evidence without graphics. */
public class StrahdChapterContentTest {
    private Path root() {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isDirectory(root.resolve("forge-gui/res/adventure"))) root = root.getParent();
        if (root == null) throw new AssertionError("Adventure resources not found");
        return root.resolve("forge-gui/res/adventure/CurseOfStrahd");
    }
    @BeforeClass public void initializeText() {
        Localizer.getInstance().initialize("en-US", root().getParent().getParent().resolve("languages").toString());
    }
    private JsonValue read(String file) throws Exception {
        Path path = root().resolve(file);
        assertTrue(Files.isRegularFile(path), "Missing Strahd resource: " + file);
        return new JsonReader().parse(Files.readString(path));
    }
    @Test public void isolatedCampaignAndFixedFortyCardStarts() throws Exception {
        JsonValue manifest = read("campaign.json"), config = read("config.json");
        assertEquals(manifest.getString("id"), "fork.curse-of-strahd");
        assertEquals(manifest.getString("version"), "0.1.0");
        for (String key : List.of("inheritCommonWorld", "inheritCommonQuests", "allowNewGamePlus", "development")) assertFalse(manifest.getBoolean(key));
        assertEquals(Set.of(config.get("allowedEditions").asStringArray()), Set.of("ISD", "DKA", "AVR", "SOI", "EMN", "MID", "VOW", "AFR", "CLB"));
        assertEquals(Set.of(config.get("colorIds").asStringArray()), Set.of("W", "B"));
        JsonValue difficulty = config.get("difficulties").get(0);
        assertEquals(difficulty.getFloat("goldLoss"), 0f);
        assertEquals(difficulty.getFloat("lifeLoss"), 0f);
        for (JsonValue deck : difficulty.get("constructedStarterDecks")) {
            int count = 0;
            for (String line : Files.readAllLines(root().resolve(deck.asString())))
                if (line.matches("[0-9]+ .+")) count += Integer.parseInt(line.substring(0, line.indexOf(' ')));
            assertEquals(count, 40, deck.asString());
        }
        EnemyData[] enemies = new Json().fromJson(EnemyData[].class, Files.readString(root().resolve("world/enemies.json")));
        assertEquals(Arrays.stream(enemies).map(e -> e.name).collect(java.util.stream.Collectors.toSet()), Set.of("Svalich Roadwarden", "Doru", "Mist-Stalked Pack"));
        for (EnemyData enemy : enemies) assertEquals(enemy.practice, enemy.name.equals("Svalich Roadwarden"));
    }
    @Test public void trackedObjectivesStayWithinTheCompactHudTextBudget() throws Exception {
        // Native 1080p/30-FPS probe showed long tracked-stage prose below the HUD edge.
        // Keep detailed guidance in NPC dialogue and reserve this space for the next target.
        for (JsonValue quest : read("world/quests.json")) {
            for (JsonValue stage : quest.get("stages")) {
                String label = stage.getString("name");
                String objective = stage.getString("description");
                assertFalse(label.isBlank(), "Tracked stage needs a label");
                assertFalse(objective.isBlank(), "Tracked stage needs an objective");
                assertTrue(label.length() <= 20, "HUD label exceeds 20 characters: " + label);
                assertTrue(objective.length() <= 40, "HUD objective exceeds 40 characters: " + objective);
                assertFalse(label.contains("\n") || objective.contains("\n"), "HUD objective must be one compact sentence");
            }
        }
    }
    @Test public void lateBriefingReconcilesAnAlreadyResolvedChurch() throws Exception {
        AdventureQuestData quest = new Json().fromJson(AdventureQuestData[].class, Files.readString(root().resolve("world/quests.json")))[0];
        assertEquals(quest.getID(), 8101);
        String[] flags = {"cosBriefed", "cosDoruSubdued", "cosChurchResolved", "cosEscortAccepted", "cosRoadCleared", "cosChapterOneComplete"};
        assertEquals(quest.stages.length, flags.length);
        Map<String, Integer> evidence = new HashMap<>(Map.of("cosDoruSubdued", 1, "cosChurchResolved", 1));
        for (int i = 0; i < flags.length; i++) {
            assertEquals(quest.stages[i].mapFlag, flags[i]);
            assertEquals(quest.stages[i].objective, AdventureQuestController.ObjectiveTypes.QuestFlag);
        }
        AdventureQuestProgression.activateNextStages(quest, s -> s.reconcilePersistentFlag(evidence.get(s.mapFlag)));
        assertFalse(quest.completed);
        evidence.put("cosBriefed", 1);
        AdventureQuestProgression.activateNextStages(quest, s -> s.reconcilePersistentFlag(evidence.get(s.mapFlag)));
        for (int i = 0; i < 3; i++) assertEquals(quest.stages[i].getStatus(), AdventureQuestController.QuestStatus.COMPLETE);
        assertEquals(quest.stages[3].getStatus(), AdventureQuestController.QuestStatus.ACTIVE);
        for (String flag : List.of("cosEscortAccepted", "cosRoadCleared")) evidence.put(flag, 1);
        AdventureQuestProgression.activateNextStages(quest, s -> s.reconcilePersistentFlag(evidence.get(s.mapFlag)));
        assertFalse(quest.completed, "Travel alone cannot finish the chapter");
        evidence.put("cosChapterOneComplete", 1);
        AdventureQuestProgression.activateNextStages(quest, s -> s.reconcilePersistentFlag(evidence.get(s.mapFlag)));
        assertTrue(quest.completed);
    }
    private boolean matches(JsonValue node, Map<String, Integer> flags) {
        JsonValue conditions = node.get("condition");
        if (conditions == null) return true;
        for (JsonValue c : conditions) {
            boolean result = true;
            for (String type : List.of("Quest", "Character")) {
                String check = "check" + type + "Flag";
                if (c.has(check)) result &= flags.getOrDefault(c.getString(check), 0) != 0;
                JsonValue query = c.get("get" + type + "Flag");
                if (query != null) result &= flags.getOrDefault(query.getString("key"), 0) == query.getInt("val");
            }
            if (c.getBoolean("not", false)) result = !result;
            if (!result) return false;
        }
        return true;
    }
    private JsonValue entry(String key, String kind, Map<String, Integer> flags) throws Exception {
        JsonValue entries = read("story/dialogues.json").get(key).get(kind);
        for (JsonValue node : entries) if (matches(node, flags)) return node;
        fail("No visible dialogue for " + key + " " + flags); return null;
    }
    private void apply(JsonValue node, Map<String, Integer> flags) {
        JsonValue actions = node.get("action");
        if (actions != null) for (JsonValue action : actions)
            for (String type : List.of("Quest", "Character")) {
                JsonValue flag = action.get("set" + type + "Flag");
                if (flag != null) flags.put(flag.getString("key"), flag.getInt("val"));
            }
    }
    private boolean hasAction(JsonValue node, String actionKey) {
        JsonValue actions = node.get("action");
        if (actions != null) for (JsonValue action : actions) if (action.has(actionKey)) return true;
        JsonValue options = node.get("options");
        if (options != null) for (JsonValue option : options) if (hasAction(option, actionKey)) return true;
        return false;
    }
    @Test public void bothDoruChoicesCommitFateAndResolutionAndCanResumeAfterVictory() throws Exception {
        for (int fate : List.of(1, 2)) {
            Map<String, Integer> flags = new HashMap<>();
            apply(entry("church.doru", "defeatDialog", flags), flags);
            assertEquals(flags.get("cosDoruSubdued"), Integer.valueOf(1));
            JsonValue resumed = entry("church.doru", "dialog", flags);
            JsonValue choice = resumed.get("options").get(fate - 1);
            apply(choice, flags);
            assertEquals(flags.get("cosDoruFate"), Integer.valueOf(fate));
            assertEquals(flags.get("cosChurchResolved"), Integer.valueOf(1));
            assertFalse(hasAction(entry("church.doru", "dialog", flags), "battleWithActorID"));
            assertTrue(entry("church.donavich", "dialog", flags).getString("text").length() > 20);
        }
    }
    @Test public void evaRequiresEscortAndRoadVictoryAndAnExplicitChoice() throws Exception {
        Map<String, Integer> flags = new HashMap<>();
        assertFalse(hasAction(entry("pool.eva", "dialog", flags), "setQuestFlag"));
        flags.put("cosRoadCleared", 1);
        assertFalse(hasAction(entry("pool.eva", "dialog", flags), "setQuestFlag"));
        flags.put("cosEscortAccepted", 1);
        JsonValue eva = entry("pool.eva", "dialog", flags);
        apply(eva, flags);
        assertFalse(flags.containsKey("cosChapterOneComplete"));
        apply(eva.get("options").get(0), flags);
        assertEquals(flags.get("cosChapterOneComplete"), Integer.valueOf(1));
        assertFalse(hasAction(entry("pool.eva", "dialog", flags), "setQuestFlag"));
    }
    @Test public void trainingChoicesCommitTwoCardsAndOnceOnlyMarkerTogether() throws Exception {
        JsonValue story = read("story/dialogues.json");
        assertEquals(story.size, 13);
        Map<String, Integer> flags = new HashMap<>();
        JsonValue reward = entry("village.practice", "defeatDialog", flags);
        Set<String> cards = new HashSet<>();
        for (JsonValue option : reward.get("options")) {
            JsonValue action = option.get("action").get(0);
            assertEquals(action.get("setQuestFlag").getString("key"), "cosTrainingRewarded");
            assertEquals(action.get("setQuestFlag").getInt("val"), 1);
            JsonValue card = action.get("grantRewards").get(0);
            assertEquals(card.getInt("count"), 2);
            cards.add(card.getString("cardName"));
        }
        assertEquals(cards, Set.of("Rebuke", "Murder"));
        flags.put("cosTrainingRewarded", 1);
        assertFalse(hasAction(entry("village.practice", "defeatDialog", flags), "grantRewards"));
        assertTrue(hasAction(entry("village.practice", "dialog", flags), "battleWithActorID"));
    }

    @Test public void wolvesAreRemovedAtVictoryWithoutAFollowupConversation() throws Exception {
        JsonValue defeated = read("story/dialogues.json").get("road.wolves").get("defeatDialog").get(0);
        assertEquals(defeated.get("action").get(0).getInt("deleteMapObject", 0), -1);
        assertTrue(defeated.getString("text", "").isEmpty());
        assertEquals(defeated.get("options").size, 0);
        assertEquals(read("story/dialogues.json").get("road.wolves").getString("removeWhenQuestFlag"), "cosRoadCleared");
    }

    @Test public void doruRemainsForHisChoiceThenLeavesAfterItsFinalLine() throws Exception {
        JsonValue encounter = read("story/dialogues.json").get("church.doru");
        for (JsonValue action : encounter.get("defeatDialog").get(0).get("action")) assertFalse(action.has("deleteMapObject"));
        for (String kind : List.of("dialog", "defeatDialog")) {
            JsonValue choice = kind.equals("dialog") ? encounter.get(kind).get(1) : encounter.get(kind).get(0);
            for (JsonValue fate : choice.get("options")) {
                assertTrue(hasAction(fate.get("options").get(0), "deleteMapObject"), "Final fate line must remove Doru");
            }
        }
        assertEquals(encounter.getString("removeWhenQuestFlag"), "cosChurchResolved");
    }

    @Test public void strahdRidesAwayOnlyAfterTheConversationIsDismissed() throws Exception {
        JsonValue encounter = read("story/dialogues.json").get("road.strahd");
        JsonValue root = encounter.get("dialog").get(0);
        assertNull(root.get("action"), "Departure must wait for the final choice");
        assertNotNull(root.get("options").get(0).get("action"), "Final choice must initiate departure");
        JsonValue action = root.get("options").get(0).get("action").get(0);
        JsonValue depart = action.get("departMapObject");
        assertNotNull(depart, "Strahd needs a mounted departure action");
        assertEquals(depart.getInt("id"), -1);
        assertTrue(depart.getFloat("duration") > 0f);
        assertTrue(Files.isRegularFile(root().getParent().resolve("common").resolve(depart.getString("mountSprite"))));
        assertEquals(action.get("setQuestFlag").getString("key"), "cosStrahdDeparted");
        assertEquals(encounter.getString("removeWhenQuestFlag"), "cosStrahdDeparted");
    }
}
