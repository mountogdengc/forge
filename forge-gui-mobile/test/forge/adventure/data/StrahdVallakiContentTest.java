package forge.adventure.data;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import forge.adventure.campaign.CampaignRegistry;
import forge.adventure.util.AdventureQuestController;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.*;

/** Contract checks for the authored second chapter, independent of rendering. */
public class StrahdVallakiContentTest {
    private static final String[] FLAGS = {"cosWestRoadCleared", "cosVallakiEntered",
            "cosBonesInvestigation", "cosCoffinLead", "cosHenrikConfessed", "cosCoffinCleared",
            "cosBonesRecovered", "cosSanctuaryDefended", "cosBonesRestored", "cosChapterTwoComplete"};

    @BeforeClass public void initializeText() {
        forge.util.Localizer.getInstance().initialize("en-US", root().getParent().getParent().resolve("languages").toString());
    }

    private Path root() {
        Path path = Path.of("").toAbsolutePath();
        while (path != null && !Files.isDirectory(path.resolve("forge-gui/res/adventure"))) path = path.getParent();
        if (path == null) throw new AssertionError("Missing Adventure resources");
        return path.resolve("forge-gui/res/adventure/CurseOfStrahd");
    }

    private JsonValue read(String name) throws Exception {
        return new JsonReader().parse(Files.readString(root().resolve(name)));
    }

    private boolean matches(JsonValue node, Map<String, Integer> flags) {
        JsonValue conditions = node.get("condition");
        if (conditions == null) return true;
        for (JsonValue c : conditions) {
            boolean match = true;
            for (String type : List.of("Quest", "Character")) {
                if (c.has("check" + type + "Flag")) match &= flags.getOrDefault(c.getString("check" + type + "Flag"), 0) != 0;
                JsonValue query = c.get("get" + type + "Flag");
                if (query != null) {
                    assertEquals(query.getString("op"), "=");
                    match &= flags.getOrDefault(query.getString("key"), 0) == query.getInt("val");
                }
            }
            if (c.getBoolean("not", false)) match = !match;
            if (!match) return false;
        }
        return true;
    }

    private JsonValue entry(String key, String kind, Map<String, Integer> flags) throws Exception {
        JsonValue actor = read("story/dialogues.json").get(key);
        assertNotNull(actor, "Missing second-chapter actor " + key);
        JsonValue result = null;
        for (JsonValue node : actor.get(kind)) if (matches(node, flags)) {
            assertNull(result, "Multiple runtime dialogue roots for " + key + " " + flags);
            result = node;
        }
        assertNotNull(result, "No dialogue for " + key + " " + flags);
        return result;
    }

    private void apply(JsonValue node, Map<String, Integer> flags) {
        JsonValue actions = node.get("action");
        if (actions != null) for (JsonValue action : actions) for (String type : List.of("Quest", "Character")) {
            JsonValue flag = action.get("set" + type + "Flag");
            if (flag != null) flags.put(flag.getString("key"), flag.getInt("val"));
        }
    }

    private int actionCount(JsonValue node, String key) {
        int count = 0;
        JsonValue actions = node.get("action"), options = node.get("options");
        if (actions != null) for (JsonValue action : actions) if (action.has(key)) count++;
        if (options != null) for (JsonValue option : options) count += actionCount(option, key);
        return count;
    }

    private Map<String, Integer> through(int count) {
        Map<String, Integer> flags = new HashMap<>(Map.of("cosChapterOneComplete", 1, "cosDoruFate", 1));
        for (int i = 0; i < count; i++) flags.put(FLAGS[i], 1);
        return flags;
    }

    private boolean setsFlag(JsonValue node, String key) {
        JsonValue actions = node.get("action"), options = node.get("options");
        if (actions != null) for (JsonValue action : actions) {
            JsonValue flag = action.get("setQuestFlag");
            if (flag != null && key.equals(flag.getString("key"))) return true;
        }
        if (options != null) for (JsonValue option : options) if (setsFlag(option, key)) return true;
        return false;
    }

    @Test public void newWorldVersionRequiresNewRunAndRetainsCampaignIdentity() throws Exception {
        assertEquals(read("campaign.json").getString("id"), "fork.curse-of-strahd");
        assertEquals(read("campaign.json").getString("version"), "0.5.0");
        var registry = CampaignRegistry.discover(root().getParent(), root().resolve("absent-user-root"), false);
        assertNotNull(registry.find("fork.curse-of-strahd", "0.5.0"));
        assertNull(registry.find("fork.curse-of-strahd", "0.4.0"));
        assertNull(registry.find("fork.curse-of-strahd", "0.3.0"));
        assertNull(registry.find("fork.curse-of-strahd", "0.2.0"));
        assertNull(registry.find("fork.curse-of-strahd", "0.1.0"), "Never silently load old worlds as new content");
        assertEquals(read("config.json").getString("chapterCompletionQuestFlag"), "cosChapterThreeComplete");
    }

    @Test public void evaHandsOffOneQuestOnlyAfterExplicitOpeningCompletion() throws Exception {
        Map<String, Integer> flags = new HashMap<>();
        assertEquals(actionCount(entry("pool.eva", "dialog", flags), "issueQuest"), 0);
        flags.put("cosEscortAccepted", 1);
        assertEquals(actionCount(entry("pool.eva", "dialog", flags), "issueQuest"), 0);
        flags.put("cosRoadCleared", 1);
        JsonValue warning = entry("pool.eva", "dialog", flags);
        apply(warning, flags);
        assertFalse(flags.containsKey("cosChapterOneComplete"));
        JsonValue choice = warning.get("options").get(0);
        assertEquals(actionCount(choice, "issueQuest"), 1);
        assertEquals(choice.get("action").get(0).getString("issueQuest"), "8102");
        apply(choice, flags);
        assertEquals(flags.get("cosChapterOneComplete"), Integer.valueOf(1));
        assertEquals(actionCount(entry("pool.eva", "dialog", flags), "issueQuest"), 0);
    }

    @Test public void tenStageJournalReconcilesEvidenceWithoutArrivalCompletingAnything() throws Exception {
        var quests = new Json().fromJson(AdventureQuestData[].class, Files.readString(root().resolve("world/quests.json")));
        assertEquals(quests.length, 3);
        AdventureQuestData quest = quests[1];
        assertEquals(quest.getID(), 8102);
        assertEquals(quest.stages.length, FLAGS.length);
        Map<String, Integer> flags = through(0);
        for (int i = 0; i < FLAGS.length; i++) {
            assertEquals(quest.stages[i].mapFlag, FLAGS[i]);
            assertEquals(quest.stages[i].objective, AdventureQuestController.ObjectiveTypes.QuestFlag);
            AdventureQuestProgression.activateNextStages(quest, s -> s.reconcilePersistentFlag(flags.get(s.mapFlag)));
            assertEquals(quest.stages[i].getStatus(), AdventureQuestController.QuestStatus.ACTIVE);
            assertFalse(quest.completed);
            flags.put(FLAGS[i], 1);
        }
        AdventureQuestProgression.activateNextStages(quest, s -> s.reconcilePersistentFlag(flags.get(s.mapFlag)));
        assertTrue(quest.completed);
    }

    @Test public void generatedVallakiQuestRetainsAutomaticJournalTracking() throws Exception {
        var quests = new Json().fromJson(AdventureQuestData[].class, Files.readString(root().resolve("world/quests.json")));
        assertTrue(quests[1].autoTrack);
        AdventureQuestData generated = new AdventureQuestData(quests[1]);
        assertTrue(generated.autoTrack, "Quest generation must retain the authored autoTrack policy");
    }

    @Test public void earlyAndOutOfOrderVisitsNeverOfferLockedBattlesOrProgress() throws Exception {
        String[] actors = {"west.ambush", "vallaki.gate", "andral.lucian", "andral.milivoj",
                "coffin.henrik", "coffin.spawn", "coffin.relic", "andral.stalker", "andral.lucian", "andral.ireena"};
        for (int i = 0; i < actors.length; i++) {
            // Missing any prerequisite must block the corresponding progression interaction.
            for (int missing = -1; missing < i; missing++) {
                Map<String, Integer> flags = through(i);
                flags.remove(missing == -1 ? "cosChapterOneComplete" : FLAGS[missing]);
                JsonValue node = entry(actors[i], "dialog", flags);
                assertEquals(actionCount(node, "battleWithActorID"), 0, actors[i] + " missing " + missing);
                assertFalse(setsFlag(node, FLAGS[i]), actors[i] + " skipped prerequisite " + missing);
                assertEquals(actionCount(node, "grantRewards"), 0, actors[i] + " missing " + missing);
            }
        }
    }

    @Test public void relicNeedsExplicitCollectionAndResumesAfterSpawnVictory() throws Exception {
        Map<String, Integer> flags = through(5);
        apply(entry("coffin.spawn", "defeatDialog", flags), flags);
        assertEquals(flags.get("cosCoffinCleared"), Integer.valueOf(1));
        assertFalse(flags.containsKey("cosBonesRecovered"));
        JsonValue relic = entry("coffin.relic", "dialog", flags);
        apply(relic, flags);
        assertFalse(flags.containsKey("cosBonesRecovered"), "Opening the box dialogue is not collection");
        apply(relic.get("options").get(0), flags);
        assertEquals(flags.get("cosBonesRecovered"), Integer.valueOf(1));
        assertEquals(actionCount(entry("coffin.relic", "dialog", flags), "setQuestFlag"), 0);
    }

    @Test public void bothTruthChoicesRestoreSanctuaryAndRemainDistinctOnRevisit() throws Exception {
        Set<String> repeatTexts = new java.util.HashSet<>();
        for (int truth : List.of(1, 2)) {
            Map<String, Integer> flags = through(8);
            JsonValue lucian = entry("andral.lucian", "dialog", flags);
            apply(lucian, flags);
            assertFalse(flags.containsKey("cosBonesRestored"));
            apply(lucian.get("options").get(truth - 1), flags);
            assertEquals(flags.get("cosBonesRestored"), Integer.valueOf(1));
            assertEquals(flags.get("cosVallakiTruth"), Integer.valueOf(truth));
            assertFalse(flags.containsKey("cosChapterTwoComplete"), "Ireena must choose sanctuary herself");
            JsonValue repeat = entry("andral.lucian", "dialog", flags);
            repeatTexts.add(repeat.getString("text"));
            assertEquals(actionCount(repeat, "setQuestFlag"), 0);
        }
        assertEquals(repeatTexts.size(), 2);
    }

    @Test public void ireenaAcknowledgesBothDoruFatesAndCommitsChosenRewardOnlyOnce() throws Exception {
        Set<String> fateTexts = new java.util.HashSet<>();
        for (int fate : List.of(1, 2)) for (int reward : List.of(0, 1)) {
            Map<String, Integer> flags = through(9);
            flags.put("cosDoruFate", fate);
            JsonValue ireena = entry("andral.ireena", "dialog", flags);
            fateTexts.add(ireena.getString("text"));
            apply(ireena, flags);
            assertFalse(flags.containsKey("cosChapterTwoComplete"));
            JsonValue choice = ireena.get("options").get(reward);
            JsonValue action = choice.get("action").get(0);
            assertEquals(action.get("setQuestFlag").getString("key"), "cosChapterTwoComplete");
            assertEquals(action.get("grantRewards").get(0).getInt("count"), 2);
            assertEquals(action.get("grantRewards").get(0).getString("cardName"), reward == 0 ? "Fiend Hunter" : "Falkenrath Noble");
            apply(choice, flags);
            assertEquals(flags.get("cosChapterTwoComplete"), Integer.valueOf(1));
            assertEquals(actionCount(entry("andral.ireena", "dialog", flags), "grantRewards"), 0);
        }
        assertEquals(fateTexts.size(), 2);
    }

    @Test public void defeatedFiguresDisappearWithoutRequiringAnotherConversation() throws Exception {
        for (String key : List.of("west.ambush", "coffin.spawn", "andral.stalker")) {
            JsonValue actor = read("story/dialogues.json").get(key);
            assertNotNull(actor, key);
            JsonValue victory = actor.get("defeatDialog").get(0);
            assertEquals(victory.getString("text"), "");
            assertEquals(victory.get("options").size, 0);
            JsonValue action = victory.get("action").get(0);
            assertEquals(action.getInt("deleteMapObject"), -1);
            assertEquals(action.get("setQuestFlag").getString("key"), actor.getString("removeWhenQuestFlag"));
        }
    }
}
