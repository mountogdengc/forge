package forge.app;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.utils.Timer;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.scene.InventoryScene;
import forge.adventure.data.ItemData;
import forge.adventure.scene.StartScene;
import forge.adventure.stage.GameHUD;
import forge.adventure.util.Current;
import forge.adventure.world.WorldSave;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.lwjgl.glfw.GLFW;

/** Noninteractive native checks; never displays a window or uses a normal player profile. */
public final class AdventureUpstreamSmoke {
    private final Path output = Path.of(System.getProperty("upstream.output"));
    private final long started = System.nanoTime();
    private int phase, settled, life, gold;
    private String deck;
    private InventoryScene inventory;
    private ScrollPane description;
    private long frame = -1;

    public static void main(String[] args) {
        require(Boolean.getBoolean("upstream.isolatedProfile"), "Disposable profile required");
        AdventureUpstreamSmoke probe = new AdventureUpstreamSmoke();
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Upstream Adventure validation");
        boolean portrait = Boolean.getBoolean("upstream.portrait");
        config.setWindowedMode(portrait ? 1080 : 1920, portrait ? 1920 : 1080);
        config.setInitialVisible(false);
        config.disableAudio(true);
        config.setForegroundFPS(30);
        config.setIdleFPS(15);
        config.setPauseWhenLostFocus(false);
        config.setPauseWhenMinimized(false);
        config.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override public void created(Lwjgl3Window window) {
                Timer.schedule(new Timer.Task() {
                    @Override public void run() { probe.tick(); }
                }, 1f, .25f);
            }
        });
        String assets = Path.of(System.getProperty("upstream.source"), "forge-gui")
                .toString().replace('\\', '/') + "/";
        ApplicationListener app = Forge.getApp(null, new Lwjgl3Clipboard(),
                new Main.DesktopAdapter(probe.output.resolve("orientation.ini").toString()) {
                    @Override public String getVersionString() { return "2.0.16-GITAdventureFixes"; }
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
            require((System.nanoTime() - started) / 1_000_000_000 < 300, "Native test timeout at " + phase);
            long handle = ((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle();
            require(GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_VISIBLE) == GLFW.GLFW_FALSE,
                    "Test window became visible");
            require(GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_FOCUSED) == GLFW.GLFW_FALSE,
                    "Test window acquired focus");
            Gdx.graphics.requestRendering();
            if (frame == Gdx.graphics.getFrameId() || Forge.getTransitionScreen() != null) return;
            frame = Gdx.graphics.getFrameId();
            if (phase == 0) {
                if (!(Forge.getCurrentScene() instanceof StartScene)) {
                    if (Forge.afterDBloaded && Forge.getSplashScreen() != null
                            && Forge.getSplashScreen().isShowModeSelector()) {
                        Forge.clearSplashScreen();
                        Forge.switchToAdventure();
                    }
                    return;
                }
                Forge.clearSplashScreen();
                Forge.clearCurrentScreen();
                Forge.startContinuousRendering();
                require(StartScene.instance().Continue(), "Copied legacy Continue failed");
                phase = 1;
                return;
            }
            if (phase == 1) {
                if (!WorldSave.hasActivePlaythrough() || !WorldSave.explorationSaveBlockReason().isEmpty()) return;
                life = Current.player().getLife();
                gold = Current.player().getGold();
                deck = Current.player().getSelectedDeck().getMain().toCardList("\n");
                Actor notifications = (Actor) field(GameHUD.class, "notificationPane").get(GameHUD.getInstance());
                Actor minimap = (Actor) field(GameHUD.class, "mapGroup").get(GameHUD.getInstance());
                require(notifications.getParent() == minimap.getParent()
                        && notifications.getZIndex() > minimap.getZIndex(),
                        "Notifications are still behind the minimap");
                // Distinct buffers prove that forced captures occur now, including two within the cooldown.
                Gdx.gl.glClearColor(1, 0, 0, 1);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                WorldSave.requestPreview(true);
                require((WorldSave.getCurrentSave().header.preview.getPixel(2, 2) & 0xFFFFFF00) == 0xFF000000,
                        "First preview did not read the current graphics buffer");
                Gdx.gl.glClearColor(0, 0, 1, 1);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                WorldSave.requestPreview(true);
                require((WorldSave.getCurrentSave().header.preview.getPixel(2, 2) & 0xFFFFFF00) == 0x0000FF00,
                        "Forced preview returned a stale image inside the cooldown");
                System.out.println("UPSTREAM_PREVIEW_AND_HUD_PASS");
                phase = 10;
                settled = 0;
                return;
            }
            if (phase == 10) {
                // Let the real world redraw after the distinctive preview test buffers.
                if (++settled < 3) return;
                inventory = InventoryScene.instance();
                Forge.switchScene(inventory);
                phase = 2;
                settled = 0;
                return;
            }
            if (phase == 2) {
                if (++settled < 4) return;
                require(Forge.getCurrentScene() == inventory, "Inventory did not open");
                TextraLabel label = (TextraLabel) field(InventoryScene.class, "itemDescription").get(inventory);
                description = (ScrollPane) field(InventoryScene.class, "itemDescriptionPane").get(inventory);
                System.out.println("DESCRIPTION_BEFORE_LAYOUT pane=" + description.getWidth() + "x" + description.getHeight()
                        + " label=" + label.getWidth() + "x" + label.getHeight());
                @SuppressWarnings("unchecked")
                var items = (java.util.Map<Button, org.apache.commons.lang3.tuple.Pair<String, ItemData>>)
                        field(InventoryScene.class, "itemLocation").get(inventory);
                Button selected = items.isEmpty() ? new Button() : items.keySet().iterator().next();
                ItemData item = new ItemData();
                item.name = "Upstream description fixture";
                item.description = "Readable description with icons [+Gold].\n".repeat(40);
                // Only the scene's disposable display mapping changes; no owned item or saved definition is edited.
                items.put(selected, org.apache.commons.lang3.tuple.Pair.of(item.name, item));
                var select = InventoryScene.class.getDeclaredMethod("setSelected", Button.class);
                select.setAccessible(true);
                select.invoke(inventory, selected);
                require(label.storedText.contains(item.name), "Selecting an item did not update the description");
                require(inventory.getUI().getStage().getScrollFocus() == description,
                        "Item selection did not focus the description pane");
                require(description.getHeight() > 0 && description.getWidth() > 0,
                        "Description has no viewport: " + description.getWidth() + "x" + description.getHeight());
                require(description.getY() >= 0 && description.getTop() <= description.getParent().getHeight(),
                        "Description viewport leaves its inventory panel");
                require(description.getMaxY() > 0, "Long description cannot scroll");
                require(description.getScrollX() == 0, "Description overflowed horizontally");
                require(!description.getActions().isEmpty(), "Long description did not offer its scroll hint");
                InputEvent scroll = new InputEvent();
                scroll.setType(InputEvent.Type.scrolled);
                scroll.setScrollAmountY(1);
                description.fire(scroll);
                require(description.getActions().isEmpty(), "Scrolling did not cancel the hint animation");
                phase = 3;
                settled = 0;
                return;
            }
            if (phase == 3) {
                if (++settled < 4) return;
                description.setScrollY(description.getMaxY());
                description.updateVisualScroll();
                require(description.getScrollY() > 0, "The description cannot reach its final lines");
                require(Current.player().getLife() == life && Current.player().getGold() == gold
                        && Current.player().getSelectedDeck().getMain().toCardList("\n").equals(deck),
                        "Opening the inventory changed the saved player's resources/deck");
                Pixmap shot = Pixmap.createFromFrameBuffer(0, 0,
                        Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
                try {
                    PixmapIO.writePNG(Gdx.files.absolute(output.resolve("inventory.png").toString()), shot, 6, true);
                } finally { shot.dispose(); }
                Files.writeString(output.resolve("PASS"),
                        "UPSTREAM_NATIVE_PASS: legacy Continue, immediate/forced GL preview, HUD z-order, inventory bounds/scroll, unchanged player\n");
                System.out.println("UPSTREAM_NATIVE_PASS");
                System.exit(0);
            }
        } catch (Throwable problem) {
            try {
                Pixmap shot = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
                try { PixmapIO.writePNG(Gdx.files.absolute(output.resolve("failure.png").toString()), shot, 6, true); }
                finally { shot.dispose(); }
            } catch (Throwable ignored) { }
            problem.printStackTrace();
            System.exit(1);
        }
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
