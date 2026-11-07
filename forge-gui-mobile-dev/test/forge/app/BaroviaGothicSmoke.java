package forge.app;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.utils.Timer;
import forge.Forge;
import forge.adventure.scene.StartScene;
import forge.adventure.scene.TileMapScene;
import forge.adventure.util.Current;
import forge.adventure.world.WorldSave;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.lwjgl.glfw.GLFW;

/** Hidden final-art integration check against a copied pre-art 0.5.0 save. */
public final class BaroviaGothicSmoke {
    private final Path output = Path.of(System.getProperty("barovia.output"));
    private final String[] destinations = {"Spawn", "Village Church", "Old Svalich Road", "Tser Pool"};
    private final String[] names = {"village", "church", "road", "tser-pool"};
    private final long started = System.nanoTime();
    private int phase, scene, settled, life, gold, frameRegion;
    private long frame = -1, animationStarted;
    private String deck;
    private AnimatedTiledMapTile fog;

    public static void main(String[] args) {
        require(Boolean.getBoolean("barovia.isolatedProfile"), "Disposable profile required");
        BaroviaGothicSmoke probe = new BaroviaGothicSmoke();
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Barovia gothic art validation");
        config.setWindowedMode(1920, 1080);
        config.setInitialVisible(false); config.disableAudio(true);
        config.setForegroundFPS(30); config.setIdleFPS(15);
        config.setPauseWhenLostFocus(false); config.setPauseWhenMinimized(false);
        config.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override public void created(Lwjgl3Window window) {
                Timer.schedule(new Timer.Task() { @Override public void run() { probe.tick(); } }, 1f, .25f);
            }
        });
        String assets = Path.of(System.getProperty("barovia.source"), "forge-gui").toString().replace('\\', '/') + "/";
        ApplicationListener app = Forge.getApp(null, new Lwjgl3Clipboard(), new Main.DesktopAdapter(probe.output.resolve("orientation.ini").toString()) {
            @Override public String getVersionString() { return "2.0.16-GITBaroviaGothic"; }
            @Override public boolean isConnectedToInternet() { return false; }
            @Override public boolean isConnectedToWifi() { return false; }
            @Override public void setMouseConfined(boolean confined) { }
            @Override public void preventSystemSleep(boolean prevent) { }
        }, assets, false, false, 0);
        Forge.selector = "Adventure";
        new Lwjgl3Application(app, config);
    }

    private void tick() {
        try {
            require((System.nanoTime() - started) / 1_000_000_000 < 600, "Art probe timeout at " + phase);
            long handle = ((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle();
            require(GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_VISIBLE) == GLFW.GLFW_FALSE, "Probe became visible");
            require(GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_FOCUSED) == GLFW.GLFW_FALSE, "Probe acquired focus");
            Gdx.graphics.requestRendering();
            if (frame == Gdx.graphics.getFrameId() || Forge.getTransitionScreen() != null) return;
            frame = Gdx.graphics.getFrameId();
            if (phase == 0) {
                if (!(Forge.getCurrentScene() instanceof StartScene)) {
                    if (Forge.afterDBloaded && Forge.getSplashScreen() != null && Forge.getSplashScreen().isShowModeSelector()) {
                        Forge.clearSplashScreen(); Forge.switchToAdventure();
                    }
                    return;
                }
                Forge.clearSplashScreen(); Forge.clearCurrentScreen(); Forge.startContinuousRendering();
                require(StartScene.instance().Continue(), "Pre-art save Continue failed");
                phase = 1; return;
            }
            if (phase == 1) {
                if (!WorldSave.explorationSaveBlockReason().isEmpty()) return;
                Properties expected = new Properties();
                try (var stream = Files.newInputStream(output.resolve("expected.properties"))) { expected.load(stream); }
                var location = WorldSave.captureExploration();
                require(location.poiId.equals(expected.getProperty("poi")) && location.playerX == Float.parseFloat(expected.getProperty("x"))
                        && location.playerY == Float.parseFloat(expected.getProperty("y")), "Old Continue changed position");
                life = Current.player().getLife(); gold = Current.player().getGold();
                require(life == Integer.parseInt(expected.getProperty("life")) && gold == Integer.parseInt(expected.getProperty("gold")), "Old Continue changed life/currency");
                require(Current.player().checkQuestFlag("cosChapterThreeComplete"), "Old Continue lost completed chapter");
                deck = Current.player().getSelectedDeck().getMain().toCardList("\n");
                require(deck.equals(expected.getProperty("deck")), "Old Continue changed deck");
                System.out.println("BAROVIA_PRE_ART_CONTINUE_PASS");
                phase = 2;
            }
            if (phase == 2) {
                var point = Current.world().getAllPointOfInterest().stream().filter(p -> p.getData().name.equals(destinations[scene])).findFirst().orElseThrow();
                TileMapScene.instance().load(point); Forge.switchScene(TileMapScene.instance());
                // This pre-art fixture has 17 life. Entering the existing town
                // intentionally recovers to its original 20-life maximum.
                if (scene == 0) {
                    require(Current.player().getLife() == 20, "Existing free town recovery changed");
                    System.out.println("BAROVIA_TOWN_RECOVERY_PASS before=" + life + " after=20");
                    life = 20;
                }
                settled = 0; phase = 3; return;
            }
            if (phase == 3) {
                if (++settled < 4) return;
                var field = TileMapScene.class.getDeclaredField("map"); field.setAccessible(true);
                TiledMap map = (TiledMap) field.get(TileMapScene.instance());
                TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get("Barovian Mist");
                require(layer != null && layer.getOpacity() <= .3f, "Missing/substantially opaque mist");
                fog = null;
                for (int y = 0; y < layer.getHeight() && fog == null; y++) for (int x = 0; x < layer.getWidth(); x++) {
                    var cell = layer.getCell(x, y);
                    if (cell != null && cell.getTile() instanceof AnimatedTiledMapTile animated) { fog = animated; break; }
                }
                require(fog != null && fog.getFrameTiles().length == 8, "Missing fog animation frames");
                if (scene == 3) {
                    TiledMapTileLayer ground = (TiledMapTileLayer) map.getLayers().get("Ground");
                    require(ground.getCell(35, 17).getTile().getId() == 23004, "Camp retained stock water");
                }
                frameRegion = region(fog); animationStarted = System.nanoTime(); phase = 4; return;
            }
            if (phase == 4) {
                if (System.nanoTime() - animationStarted < 900_000_000L) return;
                if (region(fog) == frameRegion) {
                    require(System.nanoTime() - animationStarted < 4_000_000_000L, "Fog frame clock did not advance");
                    return;
                }
                require(Current.player().getLife() == life && Current.player().getGold() == gold
                        && Current.player().getSelectedDeck().getMain().toCardList("\n").equals(deck), "Presentation visit changed player resources/deck");
                capture(names[scene]);
                System.out.println("BAROVIA_FINAL_SCENE_PASS " + names[scene] + " animated=true");
                if (++scene < destinations.length) { phase = 2; return; }
                Files.writeString(output.resolve("PASS"), "BAROVIA_GOTHIC_NATIVE_PASS: old save, four maps, animated fog, water, original town healing, unchanged gold/deck\n");
                System.out.println("BAROVIA_GOTHIC_NATIVE_PASS"); System.exit(0);
            }
        } catch (Throwable problem) { problem.printStackTrace(); System.exit(1); }
    }

    private static int region(AnimatedTiledMapTile tile) {
        var region = tile.getTextureRegion();
        return region.getRegionY() * 256 + region.getRegionX();
    }

    private void capture(String name) {
        Pixmap shot = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try { PixmapIO.writePNG(Gdx.files.absolute(output.resolve(name + ".png").toString()), shot, 6, true); }
        finally { shot.dispose(); }
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
