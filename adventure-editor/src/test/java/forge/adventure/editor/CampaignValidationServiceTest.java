package forge.adventure.editor;

import forge.CardStorageReader;
import forge.StaticData;
import forge.util.Lang;
import forge.util.Localizer;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

import static org.testng.Assert.*;

public class CampaignValidationServiceTest {
    @Test
    public void realCardNamesMissingMapsAndCachedLazyReaderWithoutGlobalDatabase() throws Exception {
        Path resources = Path.of("forge-gui/res");
        if (!Files.isDirectory(resources)) resources = Path.of("../forge-gui/res");
        resources = resources.toAbsolutePath().normalize();
        Localizer.getInstance().initialize("en-US", resources.resolve("languages").toString());
        Lang.createInstance("en-US");
        Path base = Files.createTempDirectory("campaign-validation-service");
        var databaseBefore = StaticData.instance();
        try {
            Path project = base.resolve("project"), common = base.resolve("common");
            Files.createDirectories(project.resolve("world"));
            Files.createDirectories(project.resolve("decks"));
            Files.createDirectories(project.resolve("maps"));
            Files.createDirectories(common);
            Files.writeString(project.resolve("campaign.json"), "{schemaVersion:1,id:user.test,version:0.1.0,title:Test,startingQuestIds:[],startingLocation:Spawn,inheritCommonWorld:false,inheritCommonQuests:false}");
            Files.writeString(project.resolve("world/world.json"), "{width:10,height:10,tileSize:16}");
            Files.writeString(project.resolve("world/quests.json"), "[]");
            Files.writeString(project.resolve("world/enemies.json"), "[]");
            Files.writeString(project.resolve("world/points_of_interest.json"), "[{name:Spawn,map:campaign:maps/quay.tmx}]");
            Files.writeString(project.resolve("maps/quay.tmx"), "<map/>");
            Path config = project.resolve("config.json");
            String validConfig = "{screenWidth:480,screenHeight:270,colorIds:[W],colorIdNames:[White],starterEditions:[JMP],starterEditionNames:[Jumpstart],difficulties:[{name:Normal,constructedStarterDecks:{W:decks/test.dck}}]}";
            Files.writeString(config, validConfig);
            Path deck = project.resolve("decks/test.dck");
            Files.writeString(deck, "[Main]\n1 Plains\n1 Plains!!!\n1 plains\n1 Fire\n1 Ice\n1 Fire // Ice\n1 Delver of Secrets\n1 Insectile Aberration\n1 Completely Invented Card Name 90217\n");
            CampaignValidationService service = new CampaignValidationService(resources, base.resolve("card-cache"));
            var errors = service.validate(project, common);
            assertTrue(errors.stream().anyMatch(e -> e.contains("unknown deck card Completely Invented Card Name 90217")), errors.toString());
            assertTrue(errors.stream().anyMatch(e -> e.contains("unknown deck card Plains!!!")), errors.toString());
            assertEquals(errors.size(), 2, "Only the invented name and altered punctuation should fail; actual face names and lowercase names are valid: " + errors);
            assertTrue(errors.stream().allMatch(e -> !e.contains(project.toString())), "Display project-relative diagnostics");
            var readerField = CampaignValidationService.class.getDeclaredField("cardReader");
            readerField.setAccessible(true);
            CardStorageReader reader = (CardStorageReader) readerField.get(service);
            assertNotNull(reader);
            int initialParses = reader.getNameIndexParseCount();
            Files.writeString(deck, "[Main]\n1 Plains\n");
            assertTrue(service.validate(project, common).isEmpty());
            assertSame(readerField.get(service), reader);
            assertEquals(reader.getNameIndexParseCount(), initialParses);
            for (String invalidConfig : new String[]{"{}", "[invalid-config]"}) {
                Files.writeString(config, invalidConfig);
                assertTrue(service.validate(project, common).stream().anyMatch(e -> e.startsWith("config.json:")),
                        "A config that runtime character creation rejects must not pass validation");
            }
            Files.writeString(config, validConfig);
            assertTrue(service.validate(project, common).isEmpty());
            Files.delete(project.resolve("maps/quay.tmx"));
            assertTrue(service.validate(project, common).stream().anyMatch(e -> e.contains("quay.tmx")));
            assertSame(StaticData.instance(), databaseBefore, "Validation must not instantiate a global card database");
            var cacheField = CampaignValidationService.class.getDeclaredField("cardNames");
            cacheField.setAccessible(true);
            assertEquals(((Map<?, ?>) cacheField.get(service)).size(), 9);
            try (var cacheFiles = Files.list(base.resolve("card-cache"))) { assertTrue(cacheFiles.findAny().isPresent()); }
            CampaignValidationService warm = new CampaignValidationService(resources, base.resolve("card-cache"));
            warm.validate(project, common);
            CardStorageReader warmReader = (CardStorageReader) readerField.get(warm);
            assertEquals(warmReader.getNameIndexCacheStatus(), "hit");
            assertEquals(warmReader.getNameIndexParseCount(), 0);
            assertSame(StaticData.instance(), databaseBefore);
            StringBuilder manyNames = new StringBuilder("[Main]\n");
            for (int i = 0; i < 520; i++) manyNames.append("1 Unknown Bounded Test Card ").append(i).append('\n');
            Files.writeString(deck, manyNames);
            service.validate(project, common);
            assertEquals(((Map<?, ?>) cacheField.get(service)).size(), 512);
            Files.writeString(deck, "[Main]\n1 Plains\n");
            CampaignValidationService unavailable = new CampaignValidationService(base.resolve("missing-resources"), base.resolve("unavailable-cache"));
            assertTrue(unavailable.validate(project, common).stream().anyMatch(e -> e.contains("could not complete")),
                    "A card-reader initialization failure must never be reported as a validation pass");
        } finally {
            try (var files = Files.walk(base)) {
                for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(file);
            }
        }
    }
}
