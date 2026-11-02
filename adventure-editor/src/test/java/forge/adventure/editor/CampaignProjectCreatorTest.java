package forge.adventure.editor;

import com.badlogic.gdx.utils.JsonReader;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.testng.Assert.*;

public class CampaignProjectCreatorTest {
    @Test
    public void existingCampaignMapsBecomePrivateWithoutChangingStoryOrSharedResources() throws Exception {
        Path base = Files.createTempDirectory("campaign-existing-private");
        try {
            Path campaign = fixture(base), common = base.resolve("common");
            Files.writeString(campaign.resolve("world/points_of_interest.json"),
                    "[{name:Spawn,map:../common/maps/quay.tmx,future:{value:42}},"
                            + "{name:Cellar,map:maps/cellar.tmx}]");
            Files.writeString(common.resolve("maps/cellar.tmx"),
                    "<map><properties><property name='teleport'>maps/quay.tmx</property></properties></map>");
            var unchanged = snapshot(campaign);
            var shared = snapshot(common);
            Path bundle = CampaignProjectCreator.makeMapsPrivate(campaign, common);
            assertTrue(bundle.startsWith(campaign.resolve("maps")));
            assertTrue(Files.isDirectory(bundle));
            var points = new JsonReader().parse(Files.readString(campaign.resolve("world/points_of_interest.json")));
            assertEquals(points.child.get("future").getInt("value"), 42);
            var parser = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Path quay = campaign.resolve(points.child.getString("map").substring(9));
            Path cellar = campaign.resolve(points.child.next.getString("map").substring(9));
            assertTrue(quay.startsWith(bundle));
            assertTrue(cellar.startsWith(bundle));
            var document = parser.parse(quay.toFile());
            var teleport = (org.w3c.dom.Element) document.getElementsByTagName("property").item(0);
            assertEquals(campaign.resolve(teleport.getAttribute("value")), cellar);
            var returning = (org.w3c.dom.Element) parser.parse(cellar.toFile()).getElementsByTagName("property").item(0);
            assertEquals(campaign.resolve(returning.getTextContent()), quay);
            var tileset = (org.w3c.dom.Element) document.getElementsByTagName("tileset").item(0);
            Path tsx = quay.getParent().resolve(tileset.getAttribute("source")).normalize();
            assertTrue(tsx.startsWith(bundle));
            var image = (org.w3c.dom.Element) parser.parse(tsx.toFile()).getElementsByTagName("image").item(0);
            assertTrue(Files.isRegularFile(tsx.getParent().resolve(image.getAttribute("source"))));
            for (var entry : unchanged.entrySet()) {
                if (!entry.getKey().equals(Path.of("world/points_of_interest.json")))
                    assertEquals(Files.readAllBytes(campaign.resolve(entry.getKey())), entry.getValue());
            }
            assertSnapshot(common, shared);
            var firstBundle = snapshot(bundle);
            Path nextBundle = CampaignProjectCreator.makeMapsPrivate(campaign, common);
            assertNotEquals(nextBundle, bundle);
            assertSnapshot(bundle, firstBundle);
            assertSnapshot(common, shared);
        } finally { clean(base); }
    }

    @Test
    public void existingCampaignInvalidMapLeavesCatalogAndSourceUntouched() throws Exception {
        Path base = Files.createTempDirectory("campaign-existing-invalid");
        try {
            Path campaign = fixture(base), common = base.resolve("common");
            Files.writeString(common.resolve("maps/tile.tsx"), "<tileset><image source='missing.png'/></tileset>");
            var before = snapshot(campaign);
            var shared = snapshot(common);
            assertThrows(Exception.class, () -> CampaignProjectCreator.makeMapsPrivate(campaign, common));
            assertSnapshot(campaign, before);
            assertSnapshot(common, shared);
            try (var files = Files.list(campaign)) {
                assertFalse(files.anyMatch(p -> p.getFileName().toString().startsWith(".campaign-create-")));
            }
        } finally { clean(base); }
    }

    @Test
    public void inheritedCatalogIsLocalizedWithoutWritingCommonCatalog() throws Exception {
        Path base = Files.createTempDirectory("campaign-inherited-private");
        try {
            Path campaign = fixture(base), common = base.resolve("common");
            Files.createDirectories(common.resolve("world"));
            Files.move(campaign.resolve("world/points_of_interest.json"), common.resolve("world/points_of_interest.json"));
            var shared = snapshot(common);
            CampaignProjectCreator.makeMapsPrivate(campaign, common);
            var points = new JsonReader().parse(Files.readString(campaign.resolve("world/points_of_interest.json")));
            assertTrue(Files.isRegularFile(campaign.resolve(points.child.getString("map").substring(9))));
            assertSnapshot(common, shared);
        } finally { clean(base); }
    }

    private static java.util.Map<Path, byte[]> snapshot(Path root) throws Exception {
        var result = new java.util.HashMap<Path, byte[]>();
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) result.put(root.relativize(file), Files.readAllBytes(file));
        }
        return result;
    }

    private static void assertSnapshot(Path root, java.util.Map<Path, byte[]> expected) throws Exception {
        var actual = snapshot(root);
        assertEquals(actual.keySet(), expected.keySet());
        for (var entry : expected.entrySet()) assertEquals(actual.get(entry.getKey()), entry.getValue());
    }

    @Test
    public void privateMapsIdentityUnknownFieldsAndNoOverwrite() throws Exception {
        Path base = Files.createTempDirectory("campaign-create-test");
        try {
            Path template = fixture(base), common = base.resolve("common"), projects = base.resolve("projects");
            String original = Files.readString(common.resolve("maps/quay.tmx"));
            var first = CampaignProjectCreator.create(template, common, projects, "CON", "New description");
            var second = CampaignProjectCreator.create(template, common, projects, "CON", "Other");
            assertNotEquals(first.id(), second.id());
            assertNotEquals(first.root(), second.root());
            assertTrue(first.id().startsWith("user."));
            assertTrue(first.root().getFileName().toString().length() <= 48);
            assertFalse(first.root().getFileName().toString().matches("(?i)CON(?:\\..*)?"));
            var manifest = new JsonReader().parse(Files.readString(first.root().resolve("campaign.json")));
            assertEquals(manifest.getString("version"), "0.1.0");
            assertEquals(manifest.getInt("futureField"), 42);
            assertEquals(Files.readString(first.root().resolve("config.json")), "{futureConfig:42}");
            var points = new JsonReader().parse(Files.readString(first.root().resolve("world/points_of_interest.json")));
            String map = points.child.getString("map");
            assertTrue(map.startsWith("campaign:maps/locations/"));
            Path privateMap = first.root().resolve(map.substring("campaign:".length()));
            assertTrue(Files.readString(privateMap).contains("assets/shared"));
            var xml = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(privateMap.toFile());
            var properties = xml.getElementsByTagName("property");
            var teleport = (org.w3c.dom.Element) properties.item(0);
            assertTrue(Files.isRegularFile(first.root().resolve(teleport.getAttribute("value"))));
            try (var files = Files.walk(first.root())) {
                assertTrue(files.anyMatch(p -> p.getFileName().toString().equals("tile.png")));
            }
            Files.writeString(privateMap, "edited");
            assertEquals(Files.readString(common.resolve("maps/quay.tmx")), original);
            assertEquals(new JsonReader().parse(Files.readString(template.resolve("campaign.json"))).getString("id"), "template.test");
            try (var files = Files.list(projects)) { assertEquals(files.count(), 2L); }
        } finally { clean(base); }
    }

    @Test
    public void invalidDependencyAndEscapingDestinationLeaveNothingPublished() throws Exception {
        Path base = Files.createTempDirectory("campaign-create-invalid");
        try {
            Path template = fixture(base), common = base.resolve("common"), projects = base.resolve("projects");
            Files.writeString(common.resolve("maps/tile.tsx"), "<tileset><image source='../../../escape.png'/></tileset>");
            assertThrows(Exception.class, () -> CampaignProjectCreator.create(template, common, projects, "Bad", ""));
            try (var files = Files.list(projects)) { assertEquals(files.count(), 0L); }
            assertThrows(Exception.class, () -> CampaignProjectCreator.create(template, common, template.resolve("projects"), "Bad", ""));
            assertFalse(Files.exists(template.resolve("projects")));
        } finally { clean(base); }
    }

    @Test
    public void missingCyclicAndExternalEntityDependenciesAreRejectedWithoutTouchingExistingProjects() throws Exception {
        Path base = Files.createTempDirectory("campaign-create-xml");
        try {
            Path template = fixture(base), common = base.resolve("common"), projects = base.resolve("projects");
            Files.createDirectories(projects.resolve("Existing"));
            Files.writeString(projects.resolve("Existing/keep"), "unchanged");
            for (String bad : new String[]{
                    "<tileset><image source='missing.png'/></tileset>",
                    "<tileset source='tile.tsx'/>",
                    "<!DOCTYPE tileset [<!ENTITY x SYSTEM 'file:///secret'>]><tileset>&x;</tileset>"}) {
                Files.writeString(common.resolve("maps/tile.tsx"), bad);
                assertThrows(Exception.class, () -> CampaignProjectCreator.create(template, common, projects, "Existing", ""));
                assertEquals(Files.readString(projects.resolve("Existing/keep")), "unchanged");
                try (var files = Files.list(projects)) { assertEquals(files.count(), 1L); }
            }
        } finally { clean(base); }
    }

    @Test
    public void ownedRootMapCanBeUsedAsANewTemplate() throws Exception {
        Path base = Files.createTempDirectory("campaign-create-reclone");
        try {
            Path template = fixture(base), common = base.resolve("common");
            var first = CampaignProjectCreator.create(template, common, base.resolve("projects"), "First", "");
            var second = CampaignProjectCreator.create(first.root(), common, base.resolve("projects"), "Second", "");
            var points = new JsonReader().parse(Files.readString(second.root().resolve("world/points_of_interest.json")));
            Path cloned = second.root().resolve(points.child.getString("map").substring(9));
            assertTrue(Files.isRegularFile(cloned));
            assertFalse(Files.readString(cloned).contains(first.root().toString()));
        } finally { clean(base); }
    }

    @Test
    public void installedBellwakeAndStrahdTemplatesCreatePrivateMaps() throws Exception {
        Path adventure = Path.of("forge-gui/res/adventure");
        if (!Files.isDirectory(adventure)) adventure = Path.of("../forge-gui/res/adventure");
        assertTrue(Files.isDirectory(adventure), "Run from the repository or adventure-editor module.");
        Path base = Files.createTempDirectory("campaign-installed-templates");
        try {
            for (String template : new String[]{"Bellwake", "CurseOfStrahd"}) {
                var project = CampaignProjectCreator.create(adventure.resolve(template), adventure.resolve("common"), base, template, "Created test");
                var points = new JsonReader().parse(Files.readString(project.root().resolve("world/points_of_interest.json")));
                for (var point = points.child; point != null; point = point.next) {
                    assertTrue(Files.isRegularFile(project.root().resolve(point.getString("map").substring(9))));
                }
            }
        } finally { clean(base); }
    }

    @Test
    public void incompleteOrMalformedTemplatesAreRejectedBeforeStaging() throws Exception {
        Path base = Files.createTempDirectory("campaign-template-incomplete");
        try {
            Path template = fixture(base), common = base.resolve("common"), projects = base.resolve("projects");
            Files.writeString(template.resolve("config.json"), "[]");
            assertThrows(Exception.class, () -> CampaignProjectCreator.create(template, common, projects, "Bad", ""));
            assertFalse(Files.exists(projects));
            Files.writeString(template.resolve("config.json"), "{}");
            Files.delete(template.resolve("world/enemies.json"));
            assertThrows(Exception.class, () -> CampaignProjectCreator.create(template, common, projects, "Bad", ""));
            assertFalse(Files.exists(projects));
        } finally { clean(base); }
    }

    @Test
    public void bidirectionalTeleportMapsAreClonedOnceAndPointToPrivateDestinations() throws Exception {
        Path base = Files.createTempDirectory("campaign-teleport-cycle");
        try {
            Path template = fixture(base), common = base.resolve("common");
            Files.writeString(common.resolve("maps/cellar.tmx"), "<map><properties><property name='teleport' value='maps/quay.tmx'/></properties></map>");
            var project = CampaignProjectCreator.create(template, common, base.resolve("projects"), "Doors", "");
            var points = new JsonReader().parse(Files.readString(project.root().resolve("world/points_of_interest.json")));
            Path quay = project.root().resolve(points.child.getString("map").substring(9));
            var parser = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
            var outbound = (org.w3c.dom.Element) parser.parse(quay.toFile()).getElementsByTagName("property").item(0);
            Path cellar = project.root().resolve(outbound.getAttribute("value"));
            var returning = (org.w3c.dom.Element) parser.parse(cellar.toFile()).getElementsByTagName("property").item(0);
            assertEquals(project.root().resolve(returning.getAttribute("value")), quay);
            try (var files = Files.walk(project.root())) {
                assertEquals(files.filter(p -> p.toString().endsWith(".tmx")).count(), 2L);
            }
        } finally { clean(base); }
    }

    private static Path fixture(Path base) throws Exception {
        Path template = base.resolve("template"), common = base.resolve("common");
        Files.createDirectories(template.resolve("world"));
        Files.createDirectories(common.resolve("maps"));
        Files.writeString(template.resolve("campaign.json"), "{schemaVersion:1,id:template.test,version:1,title:Template,startingQuestIds:[],startingLocation:Spawn,inheritCommonWorld:false,inheritCommonQuests:false,futureField:42}");
        Files.writeString(template.resolve("config.json"), "{futureConfig:42}");
        Files.writeString(template.resolve("world/world.json"), "{width:10,height:10,tileSize:16}");
        Files.writeString(template.resolve("world/quests.json"), "[]");
        Files.writeString(template.resolve("world/enemies.json"), "[]");
        Files.writeString(template.resolve("world/points_of_interest.json"), "[{name:Spawn,map:../common/maps/quay.tmx}]");
        Files.writeString(common.resolve("maps/quay.tmx"), "<map><tileset source='tile.tsx'/><object template='object.tx'/><properties><property name='teleport' value='maps/cellar.tmx'/></properties></map>");
        Files.writeString(common.resolve("maps/cellar.tmx"), "<map/>");
        Files.writeString(common.resolve("maps/object.tx"), "<template><tileset source='tile.tsx'/></template>");
        Files.writeString(common.resolve("maps/tile.tsx"), "<tileset><image source='tile.png'/></tileset>");
        Files.write(common.resolve("maps/tile.png"), new byte[]{1,2,3});
        return template;
    }

    private static void clean(Path root) throws Exception {
        try (var files = Files.walk(root)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(file);
        }
    }
}

