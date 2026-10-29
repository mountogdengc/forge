package forge.adventure.campaign;

import forge.adventure.data.ItemListData;
import forge.adventure.data.PointOfInterestData;
import forge.adventure.data.SettingData;
import forge.adventure.data.WorldData;
import forge.adventure.util.Config;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.testng.Assert.*;

public class CampaignRegistryTest {
    @Test
    public void discoversInstalledAndUserCampaignsByIdentityInsteadOfDisplayName() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.campaign(fixture.installed, "first-folder", "campaign.first", "Shared title", "1");
            fixture.campaign(fixture.user, "second-folder", "campaign.second", "Shared title", "2");

            CampaignRegistry registry = fixture.discover();
            assertTrue(registry.getErrors().isEmpty(), registry.getErrors().toString());
            assertEquals(registry.getCampaigns().size(), 2);
            assertEquals(registry.findById("campaign.first").getStartingQuestIds(), new String[]{"1"});
            assertEquals(registry.findById("campaign.second").getStartingQuestIds(), new String[]{"2"});
            assertEquals(registry.findById("campaign.second").getPlane(), "<user>second-folder");
            assertSame(registry.findByPlane("user_second-folder"), registry.findById("campaign.second"));
            assertNull(registry.find("campaign.first", "2"), "A save must not silently use another content version.");
        }
    }

    @Test
    public void duplicateIdentitiesAreAmbiguousAndNeitherCampaignIsOffered() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.campaign(fixture.installed, "a", "duplicate", "A", "1");
            fixture.campaign(fixture.user, "b", "duplicate", "B", "2");
            fixture.campaign(fixture.installed, "good", "valid", "Good", "3");
            CampaignRegistry registry = fixture.discover();
            assertNull(registry.findById("duplicate"));
            assertNotNull(registry.findById("valid"));
            assertEquals(registry.getCampaigns().size(), 1);
            assertTrue(registry.getErrors().stream().anyMatch(error -> error.contains("Duplicate campaign ID")));
        }
    }

    @Test
    public void explicitIdentitySurvivesFolderAndTitleRename() throws Exception {
        try (Fixture fixture = new Fixture()) {
            Path root = fixture.campaign(fixture.user, "old-folder", "unchanged-id", "Old title", "7");
            CampaignDefinition before = fixture.discover().findById("unchanged-id");
            String descriptor = Files.readString(root.resolve("campaign.json"));
            fixture.write(root.resolve("campaign.json"), descriptor.replace("Old title", "New title"));
            Files.move(root, root.resolveSibling("new-folder"));
            CampaignDefinition after = fixture.discover().findById("unchanged-id");
            assertEquals(after.getId(), before.getId());
            assertEquals(after.getVersion(), before.getVersion());
            assertEquals(after.getTitle(), "New title");
            assertEquals(after.getPlane(), "<user>new-folder");
        }
    }

    @Test
    public void openingQuestsAreExplicitAndDoNotInheritTheLegacyOpening() throws Exception {
        try (Fixture fixture = new Fixture()) {
            Path root = fixture.campaign(fixture.installed, "empty", "empty", "Empty opening", "");
            fixture.write(root.resolve("world/quests.json"), "[{\"id\":28}]");
            CampaignDefinition campaign = fixture.discover().findById("empty");
            assertEquals(campaign.getStartingQuestIds().length, 0);
            assertNull(campaign.getStartingLocation());
            assertFalse(campaign.isNewGamePlusAllowed());
            assertFalse(campaign.isLegacy());
        }
    }

    @Test
    public void rejectsMissingOpeningQuestAndMalformedDescriptorsWithoutHidingValidCampaigns() throws Exception {
        try (Fixture fixture = new Fixture()) {
            Path missing = fixture.campaign(fixture.installed, "missing-quest", "missing", "Missing", "99");
            fixture.write(missing.resolve("world/quests.json"), "[{\"id\":1}]");
            Path badSchema = fixture.campaign(fixture.installed, "bad-schema", "bad-schema", "Bad schema", "1");
            fixture.write(badSchema.resolve("campaign.json"), Files.readString(badSchema.resolve("campaign.json"))
                    .replace("\"schemaVersion\":1", "\"schemaVersion\":100"));
            Path noOpening = fixture.campaign(fixture.installed, "no-opening", "no-opening", "No opening", "1");
            fixture.write(noOpening.resolve("campaign.json"), "{\"schemaVersion\":1,\"id\":\"no-opening\","
                    + "\"version\":\"1\",\"title\":\"No opening\"}");
            fixture.campaign(fixture.installed, "good", "good", "Good", "3");

            CampaignRegistry registry = fixture.discover();
            assertEquals(registry.getCampaigns().size(), 1);
            assertNotNull(registry.findById("good"));
            assertEquals(registry.getErrors().size(), 3);
            assertTrue(registry.getErrors().stream().anyMatch(error -> error.contains("missing starting quest 99")));
            assertTrue(registry.getErrors().stream().anyMatch(error -> error.contains("Unsupported campaign descriptor")));
            assertTrue(registry.getErrors().stream().anyMatch(error -> error.contains("Declare startingQuestIds")));
        }
    }

    @Test
    public void startingQuestsMustBeUniqueTemplatesThatTheEngineCanInstantiate() throws Exception {
        try (Fixture fixture = new Fixture()) {
            Path notTemplate = fixture.campaign(fixture.installed, "not-template", "not-template", "Not a template", "1");
            fixture.write(notTemplate.resolve("world/quests.json"), "[{\"id\":1,\"isTemplate\":false}]");
            Path duplicate = fixture.campaign(fixture.installed, "duplicate-opening", "duplicate-opening", "Duplicate opening", "2");
            Path descriptor = duplicate.resolve("campaign.json");
            fixture.write(descriptor, Files.readString(descriptor).replace("[\"2\"]", "[\"2\",\"2\"]"));
            Path ambiguous = fixture.campaign(fixture.installed, "ambiguous", "ambiguous", "Ambiguous template", "3");
            fixture.write(ambiguous.resolve("world/quests.json"),
                    "[{\"id\":3,\"isTemplate\":true},{\"id\":3,\"isTemplate\":true}]");
            CampaignRegistry registry = fixture.discover();
            assertTrue(registry.getCampaigns().isEmpty());
            assertEquals(registry.getErrors().size(), 3);
        }
    }

    @Test
    public void storyAndWorldFallbackRequireAnExplicitOptIn() throws Exception {
        try (Fixture fixture = new Fixture()) {
            Path root = fixture.campaign(fixture.user, "inherited", "inherited", "Inherited", "28");
            Files.move(root.resolve("world/world.json"), fixture.common.resolve("world/world.json"));
            Files.move(root.resolve("world/quests.json"), fixture.common.resolve("world/quests.json"));
            assertNull(fixture.discover().findById("inherited"));

            Path manifest = root.resolve("campaign.json");
            fixture.write(manifest, Files.readString(manifest).replace("\"schemaVersion\":1,",
                    "\"schemaVersion\":1,\"inheritCommonWorld\":true,\"inheritCommonQuests\":true,"));
            CampaignDefinition campaign = fixture.discover().findById("inherited");
            assertNotNull(campaign);
            assertEquals(campaign.resolveResource("world/quests.json"), fixture.common.resolve("world/quests.json"));
        }
    }

    @Test
    public void reusableAssetsStillHaveCommonFallbackButCannotEscapeTheCampaign() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.campaign(fixture.installed, "local", "local", "Local", "1");
            fixture.write(fixture.common.resolve("ui/shared.txt"), "shared art");
            CampaignDefinition campaign = fixture.discover().findById("local");
            assertEquals(Files.readString(campaign.resolveResource("ui/shared.txt")), "shared art");
            assertThrows(IllegalArgumentException.class, () -> campaign.resolveResource("../../outside.txt"));
            assertThrows(IllegalArgumentException.class, () -> campaign.resolveResource("/outside.txt"));
        }
    }

    @Test
    public void legacyAdapterPreservesExistingPlanesWithoutGivingEveryPlaneQuest28() throws Exception {
        try (Fixture fixture = new Fixture()) {
            Path shandalar = fixture.campaign(fixture.installed, "Shandalar", "unused-a", "Unused", "28");
            Path other = fixture.campaign(fixture.user, "Original", "unused-b", "Unused", "42");
            Files.delete(shandalar.resolve("campaign.json"));
            Files.delete(other.resolve("campaign.json"));
            CampaignRegistry registry = fixture.discover();
            CampaignDefinition original = registry.findByPlane("<user>Original");
            assertEquals(registry.findByPlane("Shandalar").getStartingQuestIds(), new String[]{"28"});
            assertEquals(original.getStartingQuestIds().length, 0);
            assertTrue(original.isLegacy());
            assertTrue(original.isNewGamePlusAllowed());
            assertEquals(original.getStartingLocation(), "Spawn");
            assertEquals(fixture.discover().findByPlane("<user>Original").getId(), original.getId());
        }
    }

    @Test
    public void developmentCampaignsAreFilteredWithoutConfusingSameNamedUserContent() throws Exception {
        try (Fixture fixture = new Fixture()) {
            Path unfinished = fixture.campaign(fixture.installed, "Innistrad", "unused", "Unused", "28");
            Files.delete(unfinished.resolve("campaign.json"));
            fixture.campaign(fixture.user, "Innistrad", "user.innistrad", "My finished campaign", "4");
            assertEquals(fixture.discover().getCampaigns().size(), 1);
            assertNotNull(fixture.discover().findById("user.innistrad"));
            assertEquals(CampaignRegistry.discover(fixture.installed, fixture.user, true).getCampaigns().size(), 2);
        }
    }

    @Test
    public void previewAndRepeatedResourceSwitchKeepCampaignDataIsolated() throws Exception {
        Field current = Config.class.getDeclaredField("currentConfig");
        current.setAccessible(true);
        Object original = current.get(null);
        try (Fixture fixture = new Fixture()) {
            fixture.campaign(fixture.installed, "a", "a", "A", "1");
            fixture.campaign(fixture.installed, "b", "b", "B", "2");
            CampaignRegistry registry = fixture.discover();
            SettingData settings = new SettingData();
            Config first = Config.forCampaign(registry.findById("a"), settings);
            Config.activateCampaign(first);
            assertCampaignData("a");

            Config preview = Config.forCampaign(registry.findById("b"));
            assertSame(Config.instance(), first, "Opening character creation must leave Resume on its current campaign.");
            assertEquals(settings.plane, "a");
            assertEquals(preview.getFile("marker.txt").readString(), "b");
            assertCampaignData("a");

            Config.activateCampaign(preview);
            assertCampaignData("b");
            Config.activateCampaign(Config.forCampaign(registry.findById("a")));
            assertCampaignData("a");

            // Even another playthrough of A must not inherit mutable template data from the old run.
            WorldData.getAllEnemies().first().name = "mutated by old run";
            Config.activateCampaign(Config.forCampaign(registry.findById("a")));
            assertCampaignData("a");
        } finally {
            current.set(null, original);
        }
    }

    private static void assertCampaignData(String identity) {
        assertEquals(Config.instance().getFile("marker.txt").readString(), identity);
        assertEquals(ItemListData.getItem("shared-item").description, identity);
        assertEquals(WorldData.getAllEnemies().first().name, identity);
        assertEquals(WorldData.getShopList().first().name, identity);
        assertEquals(PointOfInterestData.getAllPointOfInterest().first().name, identity);
    }

    private static final class Fixture implements AutoCloseable {
        private final Path root = Files.createTempDirectory("forge-campaign-test-");
        private final Path installed = Files.createDirectories(root.resolve("installed"));
        private final Path user = Files.createDirectories(root.resolve("userplanes"));
        private final Path common = Files.createDirectories(installed.resolve("common"));

        private Fixture() throws IOException {
            Files.createDirectories(common.resolve("world"));
            write(common.resolve("config.json"), "{\"screenWidth\":480,\"screenHeight\":270,"
                    + "\"colorIds\":[\"W\"],\"colorIdNames\":[\"White\"],\"starterEditions\":[],"
                    + "\"starterEditionNames\":[],\"difficulties\":[{\"name\":\"Normal\"}]}");
        }

        private Path campaign(Path parent, String folder, String id, String title, String quest) throws IOException {
            Path path = Files.createDirectories(parent.resolve(folder));
            write(path.resolve("campaign.json"), "{\"schemaVersion\":1,\"id\":\"" + id
                    + "\",\"version\":\"1\",\"title\":\"" + title + "\",\"startingQuestIds\":["
                    + (quest.isEmpty() ? "" : "\"" + quest + "\"") + "]}");
            write(path.resolve("world/world.json"), "{\"width\":8,\"height\":8,\"tileSize\":16}");
            write(path.resolve("world/quests.json"), quest.isEmpty() ? "[]" : "[{\"id\":" + quest + ",\"isTemplate\":true}]");
            write(path.resolve("world/items.json"), "[{\"name\":\"shared-item\",\"description\":\"" + folder + "\"}]");
            for (String resource : List.of("enemies", "shops", "points_of_interest")) {
                write(path.resolve("world/" + resource + ".json"), "[{\"name\":\"" + folder + "\"}]");
            }
            write(path.resolve("marker.txt"), folder);
            return path;
        }

        private void write(Path path, String text) throws IOException {
            Files.createDirectories(path.getParent());
            Files.writeString(path, text);
        }

        private CampaignRegistry discover() { return CampaignRegistry.discover(installed, user, false); }

        @Override public void close() throws IOException {
            try (Stream<Path> files = Files.walk(root)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }
}
