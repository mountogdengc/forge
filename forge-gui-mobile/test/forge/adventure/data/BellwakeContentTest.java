package forge.adventure.data;

import com.badlogic.gdx.utils.Json;
import forge.adventure.util.AdventureQuestController;
import forge.adventure.campaign.CampaignContentValidator;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import forge.util.Localizer;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.*;
import static forge.adventure.util.AdventureQuestController.QuestStatus.*;

public class BellwakeContentTest {
    @Test
    public void authoredResourcesAndCardReferencesResolve() {
        Path cards = adventure().getParent().resolve("cardsfolder");
        var errors = CampaignContentValidator.validate(adventure().resolve("Bellwake"), adventure().resolve("common"), name -> {
            String stem = name.toLowerCase(java.util.Locale.ROOT).replace("'", "").replace('-', '_').replace(' ', '_');
            return Files.isRegularFile(cards.resolve(stem.substring(0, 1)).resolve(stem + ".txt"));
        });
        assertTrue(errors.isEmpty(), errors.toString());
    }
    @BeforeClass
    public void initializeText() {
        Localizer.getInstance().initialize("en-US", adventure().getParent().resolve("languages").toString());
    }
    private Path adventure() {
        Path root = Path.of("").toAbsolutePath();
        while (root != null && !Files.isDirectory(root.resolve("forge-gui/res/adventure"))) root = root.getParent();
        if (root == null) throw new AssertionError("Adventure resources not found");
        return root.resolve("forge-gui/res/adventure");
    }

    private AdventureQuestData quest() throws Exception {
        return new Json().fromJson(AdventureQuestData[].class,
                Files.readString(adventure().resolve("Bellwake/world/quests.json")))[0];
    }

    @Test
    public void storyRequiresBriefingTicketAndIntentionalResolutionEvenWhenTicketWasFoundEarly() throws Exception {
        AdventureQuestData quest = quest();
        assertEquals(quest.stages.length, 3, "Return must be an interaction, not a travel epilogue");
        for (AdventureQuestStage stage : quest.stages)
            assertEquals(stage.objective, AdventureQuestController.ObjectiveTypes.QuestFlag);
        Map<String, Integer> evidence = new HashMap<>();
        evidence.put("bellwakeFareReleased", 1);
        AdventureQuestProgression.activateNextStages(quest, s -> s.reconcilePersistentFlag(evidence.get(s.mapFlag)));
        assertFalse(quest.completed);
        assertEquals(quest.stages[0].getStatus(), ACTIVE);
        assertEquals(quest.stages[1].getStatus(), INACTIVE);
        evidence.put("bellwakeBriefed", 1);
        AdventureQuestProgression.activateNextStages(quest, s -> s.reconcilePersistentFlag(evidence.get(s.mapFlag)));
        assertEquals(quest.stages[0].getStatus(), COMPLETE);
        assertEquals(quest.stages[1].getStatus(), COMPLETE);
        assertEquals(quest.stages[2].getStatus(), ACTIVE);
        assertFalse(quest.completed);
        evidence.put("bellwakeResolved", 1);
        AdventureQuestProgression.activateNextStages(quest, s -> s.reconcilePersistentFlag(evidence.get(s.mapFlag)));
        assertTrue(quest.completed);
    }

    @Test
    public void bothEndingInteractionsCommitOutcomeRewardAndResolutionTogether() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var properties = factory.newDocumentBuilder().parse(adventure().resolve("common/maps/map/bellwake/quay.tmx").toFile())
                .getElementsByTagName("property");
        Map<Integer, String> endings = new HashMap<>();
        for (int i = 0; i < properties.getLength(); i++) {
            var node = properties.item(i);
            if (!node.getAttributes().getNamedItem("name").getNodeValue().equals("dialog")) continue;
            DialogData[] dialogs = new Json().fromJson(DialogData[].class, node.getTextContent());
            for (DialogData dialog : dialogs) collectEndings(dialog, endings);
        }
        assertEquals(endings, Map.of(1, "Ghoulcaller's Bell", 2, "Traveler's Amulet"));
    }

    private void collectEndings(DialogData dialog, Map<Integer, String> endings) {
        for (DialogData.ActionData action : dialog.action) {
            if (action.setCharacterFlag != null && action.setCharacterFlag.key.equals("bellwakeEnding")) {
                assertNotNull(action.setQuestFlag);
                assertEquals(action.setQuestFlag.key, "bellwakeResolved");
                assertEquals(action.setQuestFlag.val, 1);
                assertEquals(action.grantRewards.length, 1);
                assertEquals(action.grantRewards[0].count, 1);
                assertNull(endings.put(action.setCharacterFlag.val, action.grantRewards[0].cardName));
            }
        }
        for (DialogData option : dialog.options) {
            assertFalse(option.name.isBlank(), "A chapter choice must have a visible label");
            collectEndings(option, endings);
        }
    }

    @Test
    public void curatedStartsArePlayableDecksAndDefeatCannotConsumeTheirResources() throws Exception {
        Path configFile = adventure().resolve("Bellwake/config.json");
        assertTrue(Files.isRegularFile(configFile), "Bellwake must own its starter/difficulty configuration");
        ConfigData config = new Json().fromJson(ConfigData.class, Files.readString(configFile));
        assertEquals(Set.of(config.colorIds), Set.of("W", "R"));
        assertEquals(config.difficulties.length, 1);
        DifficultyData difficulty = config.difficulties[0];
        assertEquals(difficulty.startingLife, 20);
        assertEquals(difficulty.goldLoss, 0f);
        assertEquals(difficulty.lifeLoss, 0f);
        assertNull(difficulty.starterDecks);
        assertNull(difficulty.pileDecks);
        assertNull(difficulty.commanderDecks);
        for (String color : config.colorIds) {
            Path deck = adventure().resolve("Bellwake").resolve(difficulty.constructedStarterDecks.get(color));
            int count = 0;
            Map<String, Integer> copies = new HashMap<>();
            for (String line : Files.readAllLines(deck)) {
                if (line.matches("[0-9]+ .+")) {
                    int quantity = Integer.parseInt(line.substring(0, line.indexOf(' ')));
                    count += quantity;
                    copies.merge(line.substring(line.indexOf(' ') + 1), quantity, Integer::sum);
                }
            }
            assertEquals(count, 40, "Starter deck size: " + color);
            String upgrade = color.equals("W") ? "Pacifism" : "Lightning Strike";
            assertTrue(copies.getOrDefault(upgrade, 0) + 2 <= 4, "Both practice reward copies must fit the deck's copy limit");
        }
    }
}
