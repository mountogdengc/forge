package forge.adventure.campaign;

import org.testng.annotations.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.testng.Assert.*;

public class CampaignContentValidatorTest {
    private Path fixture(String quests, String dialog, String deck) throws Exception {
        Path root = Files.createTempDirectory("bellwake-content-");
        Path chapter = root.resolve("chapter");
        Path common = root.resolve("common");
        Files.createDirectories(chapter.resolve("world"));
        Files.createDirectories(chapter.resolve("decks"));
        Files.createDirectories(common.resolve("maps"));
        Files.writeString(chapter.resolve("config.json"), "{difficulties:[]}");
        Files.writeString(chapter.resolve("campaign.json"), "{startingQuestIds:[1],startingLocation:Home}");
        Files.writeString(chapter.resolve("world/quests.json"), quests);
        Files.writeString(chapter.resolve("world/enemies.json"), "[{name:Keeper,deck:[decks/keeper.dck]}]");
        Files.writeString(chapter.resolve("world/points_of_interest.json"), "[{name:Home,map:maps/home.tmx}]");
        Files.writeString(chapter.resolve("decks/keeper.dck"), deck);
        Files.writeString(common.resolve("maps/home.tmx"), "<map><objectgroup><object id=\"60\"><properties><property name=\"enemy\" value=\"Keeper\"/><property name=\"dialog\">" + dialog + "</property></properties></object></objectgroup></map>");
        return root;
    }

    @Test
    public void acceptsForgeJsonSyntaxAndResolvedReferences() throws Exception {
        Path root = fixture("[{id:1,stages:[{id:1,objective:QuestFlag,mapFlag:done}]}]",
                "[{text:Hello,options:[{name:Continue}]}]", "[Main]\n4 Known Card\n");
        try { assertEquals(CampaignContentValidator.validate(root.resolve("chapter"), root.resolve("common"), "Known Card"::equals), List.of()); }
        finally { remove(root); }
    }

    @Test
    public void identifiesUnreachableStageUnlabelledChoiceAndUnknownCardWithTheirSources() throws Exception {
        Path root = fixture("[{id:1,stages:[{id:2,prerequisiteIDs:[99]}]}]",
                "[{text:Hello,options:[{name:\"\"}]}]", "[Main]\n4 Missing Card\n");
        try {
            List<String> errors = CampaignContentValidator.validate(root.resolve("chapter"), root.resolve("common"), "Known Card"::equals);
            assertTrue(errors.stream().anyMatch(e -> e.contains("quest 1 stage 2") && e.contains("99")), errors.toString());
            assertTrue(errors.stream().anyMatch(e -> e.contains("object 60") && e.contains("label")), errors.toString());
            assertTrue(errors.stream().anyMatch(e -> e.contains("keeper.dck") && e.contains("Missing Card")), errors.toString());
        } finally { remove(root); }
    }

    @Test
    public void reportsMissingMapsAndDuplicateStages() throws Exception {
        Path root = fixture("[{id:1,stages:[{id:2},{id:2}]}]", "[]", "[Main]\n4 Known Card\n");
        try {
            Files.delete(root.resolve("common/maps/home.tmx"));
            List<String> errors = CampaignContentValidator.validate(root.resolve("chapter"), root.resolve("common"), "Known Card"::equals);
            assertTrue(errors.stream().anyMatch(e -> e.contains("home.tmx")), errors.toString());
            assertTrue(errors.stream().anyMatch(e -> e.contains("duplicate") && e.contains("stage 2")), errors.toString());
        } finally { remove(root); }
    }

    @Test
    public void reportsInformationalTextThatTheRuntimeWouldImmediatelyDismiss() throws Exception {
        Path root = fixture("[{id:1,stages:[]}]", "[{text:Hello,options:[{name:Explain,text:Details}]}]", "[Main]\n4 Known Card\n");
        try {
            var errors = CampaignContentValidator.validate(root.resolve("chapter"), root.resolve("common"), "Known Card"::equals);
            assertTrue(errors.stream().anyMatch(e -> e.contains("object 60") && e.contains("dismiss")), errors.toString());
        } finally { remove(root); }
    }

    @Test public void validatesAlternateStartersAndMapRewardAndDeckOverride() throws Exception {
        Path root = fixture("[{id:1,stages:[]}]", "[]", "[Main]\n4 Known Card\n");
        try {
            Files.writeString(root.resolve("chapter/config.json"), "{starterDecksByEdition:{SET:{W:decks/missing-edition.dck}},difficulties:[{starterDecks:{W:decks/missing-standard.dck},pileDecks:{W:decks/missing-pile.dck},commanderDecks:{W:decks/missing-commander.dck}}]}");
            Files.writeString(root.resolve("common/maps/home.tmx"), "<map><objectgroup><object id=\"60\"><properties><property name=\"reward\">[{cardName:Invented}]</property><property name=\"deckOverride\" value=\"decks/missing-override.dck\"/></properties></object></objectgroup></map>");
            var errors = CampaignContentValidator.validate(root.resolve("chapter"), root.resolve("common"), "Known Card"::equals);
            for (String expected : List.of("missing-edition", "missing-standard", "missing-pile", "missing-commander", "Invented", "missing-override"))
                assertTrue(errors.stream().anyMatch(e -> e.contains(expected)), errors.toString());
        } finally { remove(root); }
    }

    private void remove(Path root) throws Exception {
        try (var paths = Files.walk(root)) {
            for (Path path : (Iterable<Path>) paths.sorted(java.util.Comparator.reverseOrder())::iterator) Files.delete(path);
        }
    }
}
