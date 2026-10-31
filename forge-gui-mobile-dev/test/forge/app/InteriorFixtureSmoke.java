package forge.app;

import com.badlogic.gdx.utils.JsonReader;
import forge.Forge;
import forge.adventure.campaign.CampaignRegistry;
import forge.adventure.character.EntryActor;
import forge.adventure.character.ShopActor;
import forge.adventure.scene.RewardScene;
import forge.adventure.scene.TileMapScene;
import forge.adventure.stage.MapStage;
import forge.adventure.util.Config;
import forge.adventure.util.Current;
import forge.adventure.util.SaveFileData;
import forge.adventure.world.ExplorationSnapshot;
import forge.adventure.world.PlaythroughRepository;
import forge.adventure.world.SaveKind;
import forge.adventure.world.SaveSnapshot;
import forge.adventure.world.WorldSave;
import forge.adventure.world.WorldSaveHeader;
import forge.localinstance.properties.ForgeConstants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Disposable connected-room fixture: respawning enemy, two signed shops, rotating stock and anonymous exit. */
final class InteriorFixtureSmoke {
    private int step;
    private SaveSnapshot original;
    private Map<Integer, String> stock;
    private long expectedRandomDraw;

    void begin() throws Exception {
        Path fixture = Path.of(ForgeConstants.USER_ADVENTURE_DIR, "userplanes", "SmokeFailure");
        Path common = Path.of(Config.instance().getCommonFilePath("")).toAbsolutePath().normalize();
        Path floor = fixture.resolve("maps/map/smoke-floor.tmx").toAbsolutePath().normalize();
        Path root = fixture.resolve("maps/map/smoke-root.tmx").toAbsolutePath().normalize();
        Files.createDirectories(floor.getParent());
        copyMap(common.resolve("maps/map/bellwake/toll.tmx"), floor, true, common);
        copyMap(common.resolve("maps/map/bellwake/toll.tmx"), root, false, common);
        var enemies = new JsonReader().parse(Files.readString(fixture.resolve("world/enemies.json")));
        for (var enemy : enemies) if (enemy.getString("name", "").equals("Keeper of Fares")) enemy.get("boss").set(false);
        Files.writeString(fixture.resolve("world/enemies.json"), enemies.toString());
        Files.writeString(fixture.resolve("world/shops.json"), """
            [{name:FixtureA,description:White,spriteAtlas:maps/tileset/buildings.atlas,sprite:RotatingShop,
              rewards:[{type:randomCard,count:4,colors:[white],rarity:[common]}]},
             {name:FixtureB,description:Red,spriteAtlas:maps/tileset/buildings.atlas,sprite:RotatingShop,
              rewards:[{type:randomCard,count:4,colors:[red],rarity:[common]}]}]
            """);
        var campaign = CampaignRegistry.discover().getCampaigns().stream()
                .filter(c -> c.getId().equals("fixture.bellwake.failure")).findFirst().orElseThrow();
        PlaythroughRepository repository = new PlaythroughRepository(Path.of(ForgeConstants.USER_ADVENTURE_DIR, "campaign-saves"));
        original = WorldSave.listSnapshots(WorldSave.getCurrentPlaythrough().playthroughId).stream().filter(s -> s.kind == SaveKind.QUICK).findFirst().orElseThrow();
        WorldSaveHeader header;
        SaveFileData data;
        try (var input = new java.io.ObjectInputStream(new java.util.zip.InflaterInputStream(Files.newInputStream(repository.validatedPayload(original))))) {
            header = (WorldSaveHeader) input.readObject();
            data = (SaveFileData) input.readObject();
        }
        var run = repository.createPlaythrough(campaign.getId(), campaign.getVersion(), campaign.getTitle(), campaign.getPlane(), "Connected fixture", "Smoke");
        SaveSnapshot rootSave;
        try {
            rootSave = repository.writeSnapshot(repository.prepareSave(run, SaveKind.MANUAL, "Fixture root", "Root", 2), target -> {
                try (var output = new java.io.ObjectOutputStream(new java.util.zip.DeflaterOutputStream(Files.newOutputStream(target)))) {
                    output.writeObject(header);
                    output.writeObject(data);
                }
            }, target -> { });
        } finally { header.dispose(); }
        require(WorldSave.loadSnapshot(rootSave), WorldSave.getLastError());
        WorldSave.showRestoredExploration();
        TileMapScene.instance().loadNext("maps/map/smoke-floor.tmx", 38);
        require(!WorldSave.saveManual("Unsafe queued transition"), "A queued room transition was saved before it completed");
    }

    boolean tick() throws Exception {
        switch (step) {
            case 0:
                if (TileMapScene.instance().hasPendingMapTransition()) return false;
                var hierarchy = Forge.class.getDeclaredMethod("getHierachyScreen");
                hierarchy.setAccessible(true);
                System.out.println("CONNECTED_FIXTURE scene=" + Forge.getCurrentScene().getClass().getSimpleName()
                        + " screen=" + hierarchy.invoke(null) + " adventure=" + Forge.isMobileAdventureMode
                        + " map=" + MapStage.getInstance().getCurrentMap() + " pending=" + TileMapScene.instance().hasPendingMapTransition());
                require(MapStage.getInstance().getCurrentMap().equals("maps/map/smoke-floor.tmx"), "Connected floor did not load");
                require(MapStage.getInstance().lookForID(82), "Anonymous exit did not retain its TMX ID");
                stock = stocks();
                require(WorldSave.getCurrentSave().quickSave(), WorldSave.getLastError());
                BellwakeStorySmoke.victory(60);
                break;
            case 1:
                if (!(Forge.getCurrentScene() instanceof RewardScene)) return false;
                BellwakeStorySmoke.finishRewards();
                break;
            case 2:
                if (!WorldSave.explorationSaveBlockReason().isEmpty()) return false;
                require(!MapStage.getInstance().lookForID(60), "Respawning enemy was not removed for this visit");
                require(!MapStage.getInstance().getChanges().isObjectDeleted(60), "Respawning enemy was deleted permanently");
                require(WorldSave.getCurrentSave().quickSave(), WorldSave.getLastError());
                expectedRandomDraw = WorldSave.captureExploration().random.nextLong();
                require(WorldSave.getCurrentSave().quickLoad(), WorldSave.getLastError());
                WorldSave.showRestoredExploration();
                break;
            case 3:
                require(!MapStage.getInstance().lookForID(60), "Reload resurrected a per-visit defeated enemy");
                require(stock.equals(stocks()), "Reload rerolled shop identity or opened stock");
                require(Current.world().getRandom().nextLong() == expectedRandomDraw, "Reopening restored stock changed the saved random stream");
                ((EntryActor) MapStage.getInstance().getByID(38)).onPlayerCollide();
                break;
            case 4:
                if (TileMapScene.instance().hasPendingMapTransition()) return false;
                require(MapStage.getInstance().getCurrentMap().equals("maps/map/smoke-root.tmx"), "Saved floor's return exit failed");
                TileMapScene.instance().loadNext("maps/map/smoke-floor.tmx", 38);
                break;
            case 5:
                if (TileMapScene.instance().hasPendingMapTransition()) return false;
                require(MapStage.getInstance().lookForID(60), "A new visit did not respawn the ordinary enemy");
                require(WorldSave.loadSnapshot(original), WorldSave.getLastError());
                WorldSave.showRestoredExploration();
                return true;
            default: throw new AssertionError("Invalid connected fixture step");
        }
        step++;
        return false;
    }

    private static Map<Integer, String> stocks() {
        Map<Integer, String> result = new java.util.HashMap<>();
        for (int id : new int[]{80, 81}) {
            ShopActor shop = (ShopActor) MapStage.getInstance().getByID(id);
            StringBuilder signature = new StringBuilder(shop.getName()).append(':').append(shop.getRestockPrice());
            for (var reward : shop.getRewardData()) {
                signature.append('|').append(reward.getType()).append(':').append(reward.getCount());
                if (reward.getCard() != null) signature.append(':').append(reward.getCard().toString());
            }
            result.put(id, signature.toString());
        }
        return result;
    }

    private static void copyMap(Path source, Path destination, boolean floor, Path common) throws Exception {
        var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var document = factory.newDocumentBuilder().parse(source.toFile());
        var nodes = document.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) for (String attribute : new String[]{"source", "template"}) {
            var element = (org.w3c.dom.Element) nodes.item(i);
            if (element.hasAttribute(attribute)) element.setAttribute(attribute,
                    destination.getParent().relativize(source.getParent().resolve(element.getAttribute(attribute)).normalize()).toString().replace('\\', '/'));
        }
        if (floor) {
            var map = document.getDocumentElement();
            var properties = document.createElement("properties");
            map.appendChild(properties);
            property(document, properties, "respawnEnemies", "true", "bool");
            var objects = (org.w3c.dom.Element) document.getElementsByTagName("objectgroup").item(0);
            var objectNodes = document.getElementsByTagName("object");
            for (int i = 0; i < objectNodes.getLength(); i++) {
                var object = (org.w3c.dom.Element) objectNodes.item(i);
                var objectProperties = (org.w3c.dom.Element) object.getElementsByTagName("properties").item(0);
                if (object.getAttribute("id").equals("60")) {
                    var values = object.getElementsByTagName("property");
                    for (int j = values.getLength() - 1; j >= 0; j--) {
                        var value = (org.w3c.dom.Element) values.item(j);
                        if (value.getAttribute("name").equals("defeatDialog")) value.getParentNode().removeChild(value);
                    }
                } else if (object.getAttribute("id").equals("38")) {
                    property(document, objectProperties, "teleport", "maps/map/smoke-root.tmx", null);
                }
            }
            for (int id : new int[]{80, 81}) {
                var object = document.createElement("object");
                object.setAttribute("id", Integer.toString(id));
                object.setAttribute("x", Integer.toString(320 + (id - 80) * 32));
                object.setAttribute("y", "128");
                object.setAttribute("template", destination.getParent().relativize(common.resolve("maps/obj/shop.tx")).toString().replace('\\', '/'));
                var values = document.createElement("properties");
                object.appendChild(values);
                if (id == 80) {
                    property(document, values, "type", "Rotating", null);
                    property(document, values, "rotation", "FixtureA,FixtureB", null);
                } else property(document, values, "commonShopList", "FixtureA,FixtureB", null);
                objects.appendChild(object);
            }
            var exit = document.createElement("object");
            exit.setAttribute("id", "82"); exit.setAttribute("x", "400"); exit.setAttribute("y", "32");
            exit.setAttribute("width", "16"); exit.setAttribute("height", "16");
            var values = document.createElement("properties"); exit.appendChild(values);
            property(document, values, "type", "exit", null); objects.appendChild(exit);
        }
        var transformer = javax.xml.transform.TransformerFactory.newInstance().newTransformer();
        transformer.transform(new javax.xml.transform.dom.DOMSource(document), new javax.xml.transform.stream.StreamResult(destination.toFile()));
    }

    private static void property(org.w3c.dom.Document document, org.w3c.dom.Element parent, String name, String value, String type) {
        for (var node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof org.w3c.dom.Element element && element.getAttribute("name").equals(name)) {
                element.setAttribute("value", value); return;
            }
        }
        var property = document.createElement("property"); property.setAttribute("name", name); property.setAttribute("value", value);
        if (type != null) property.setAttribute("type", type); parent.appendChild(property);
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
