package forge.app;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Timer;
import forge.Forge;
import forge.adventure.campaign.*;
import forge.adventure.scene.*;
import forge.adventure.stage.*;
import forge.adventure.util.*;
import forge.adventure.world.WorldSave;
import forge.card.ColorSet;
import java.nio.file.Path;
import java.nio.file.Files;

/** Hidden native probe with a disposable profile and explicitly seeded quest progress. */
public final class QuestNavigationSmoke {
    private int phase;
    private int settledFrames;
    private long lastFrame = -1;
    private final Path output = Path.of(System.getProperty("navigation.output"));
    private final long started = System.nanoTime();

    public static void main(String[] args) {
        require(Boolean.getBoolean("navigation.isolatedProfile"), "Disposable profile required");
        QuestNavigationSmoke probe = new QuestNavigationSmoke();
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("Quest navigation validation");
        cfg.setWindowedMode(Integer.getInteger("navigation.width", 1280), Integer.getInteger("navigation.height", 720));
        cfg.setInitialVisible(false); cfg.disableAudio(true); cfg.setForegroundFPS(30);
        cfg.setPauseWhenLostFocus(false); cfg.setPauseWhenMinimized(false);
        cfg.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override public void created(Lwjgl3Window window) {
                Timer.schedule(new Timer.Task() { @Override public void run() { probe.tick(); } }, 1f, 1f);
            }
        });
        String assets = Path.of(System.getProperty("navigation.source"), "forge-gui").toString().replace('\\', '/') + "/";
        ApplicationListener app = Forge.getApp(null, new Lwjgl3Clipboard(), new Main.DesktopAdapter(probe.output.resolve("orientation.ini").toString()) {
            @Override public String getVersionString() { return "2.0.16-GITNavigationSmoke"; }
            @Override public boolean isConnectedToInternet() { return false; }
            @Override public boolean isConnectedToWifi() { return false; }
            @Override public void setMouseConfined(boolean confined) { }
            @Override public void preventSystemSleep(boolean prevent) { }
        }, assets, Integer.getInteger("navigation.height",720) > Integer.getInteger("navigation.width",1280), false, 0);
        Forge.selector = "Adventure";
        new Lwjgl3Application(app, cfg);
    }

    private void tick() {
        try {
            long frame = Gdx.graphics.getFrameId();
            if (frame == lastFrame) return;
            lastFrame = frame;
            require((System.nanoTime() - started) / 1_000_000_000 < 240, "Native navigation probe timed out at " + phase);
            Gdx.graphics.requestRendering();
            if (Forge.getTransitionScreen() != null) return;
            if (phase == 0) {
                if (!(Forge.getCurrentScene() instanceof StartScene)) {
                    if (Forge.afterDBloaded && Forge.getSplashScreen().isShowModeSelector()) {
                        Forge.clearSplashScreen(); Forge.switchToAdventure();
                    }
                    return;
                }
                Forge.clearSplashScreen(); Forge.clearCurrentScreen(); Forge.startContinuousRendering();
                CampaignDefinition campaign = CampaignRegistry.discover().getCampaigns().stream()
                        .filter(c -> c.getId().equals("fork.curse-of-strahd")).findFirst().orElseThrow();
                CampaignManager.activate(campaign);
                WorldSave.beginPlaythrough(campaign, "Navigation probe", "Navigation probe");
                WorldSave.generateNewWorld("Navigation probe", true, 0, 0, ColorSet.fromNames("W"),
                        Config.instance().getConfigData().difficulties[0], AdventureModes.Constructed, 0, null, 0);
                Forge.switchScene(GameScene.instance());
                var quest = AdventureQuestController.instance().generateQuest(8101);
                quest.prologue = null;
                // Simulate an existing save: progress is retained and the new hints are absent.
                var status = forge.adventure.data.AdventureQuestStage.class.getDeclaredField("status");
                status.setAccessible(true);
                for (int i = 0; i < quest.stages.length; i++) {
                    quest.stages[i].navigationTarget = null;
                    status.set(quest.stages[i], i == 5 ? AdventureQuestController.QuestStatus.ACTIVE : AdventureQuestController.QuestStatus.COMPLETE);
                }
                Current.player().addQuest(quest, true);
                if (MapStage.getInstance().isInMap()) MapStage.getInstance().exitDungeon(false, false);
                phase = 1; return;
            }
            if (phase == 1) {
                if (++settledFrames < 3) return;
                GameHUD hud = GameHUD.getInstance();
                Actor panel = hud.getRoot().findActor("quest-tracker");
                Label objective = hud.getRoot().findActor("tracked-quest-objective");
                System.out.println("NAV_PROBE scene=" + Forge.getCurrentScene().getClass().getSimpleName()
                        + " bounds=" + panel.getX() + "," + panel.getY() + "," + panel.getWidth() + "," + panel.getHeight()
                        + " viewport=" + hud.getWidth() + "," + hud.getHeight() + " text=" + objective.getText());
                System.out.println("NAV_POLICY " + WorldSave.explorationSaveBlockReason() + " paused="
                        + WorldStage.getInstance().isPaused() + " dialog=" + MapStage.getInstance().isDialogOnlyInput()
                        + " frozen=" + Forge.advFreezePlayerControls);
                capture("before-assert.png");
                require(panel.isVisible(), "Quest tracker is hidden");
                require(objective.getText().toString().contains("Tser Pool Encampment"), "Tracker did not resolve old-save destination");
                require(objective.getText().toString().contains("Speak to Madam Eva"), "Tracker has wrong objective");
                require(panel.getY() >= 0 && panel.getTop() <= hud.getHeight() && panel.getRight() <= hud.getWidth(), "Quest tracker exceeds viewport");
                for (String name : new String[]{"menu", "deck", "inventory", "logbook", "exittoworldmap", "bookmark"}) {
                    Actor control = hud.ui.findActor(name);
                    require(!control.isVisible() || panel.getRight() <= control.getX() || panel.getX() >= control.getRight()
                            || panel.getTop() <= control.getY() || panel.getY() >= control.getTop(), "Quest tracker covers " + name);
                }
                var arrowField = WorldStage.class.getDeclaredField("navArrow"); arrowField.setAccessible(true);
                NavArrowActor arrow = (NavArrowActor) arrowField.get(WorldStage.getInstance());
                float expectedAngle = Current.world().findPointsOfInterest("Tser Pool").getCenter()
                        .sub(WorldStage.getInstance().getPlayerSprite().getCenter()).angleDeg();
                require(arrow.isVisible() && Math.abs(arrow.navTargetAngle - expectedAngle) < 0.01f, "Arrow does not point to Tser Pool");
                capture("quest-hud.png");
                Forge.switchScene(MapViewScene.instance()); phase = 2; return;
            }
            var ui = UIScene.class.getDeclaredField("ui"); ui.setAccessible(true);
            Actor marker = ((UIActor) ui.get(MapViewScene.instance())).findActor("quest-destination-8101");
            require(marker != null, "Tracked destination marker missing");
            require(((com.github.tommyettinger.textra.TypingLabel) marker).getOriginalText().toString().contains("Tser Pool Encampment"), "Map marker names the wrong destination");
            if (phase == 2) { MapViewScene.instance().scroll(); phase = 3; return; }
            capture("quest-map.png");
            Files.writeString(output.resolve("PASS"), "QUEST_NAVIGATION_NATIVE_PASS: old-save Tser Pool destination, visible objective, named map marker\n");
            System.exit(0);
        } catch (Throwable failure) { failure.printStackTrace(); System.exit(1); }
    }

    private void capture(String name) {
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try { PixmapIO.writePNG(Gdx.files.absolute(output.resolve(name).toString()), pixels, 6, true); }
        finally { pixels.dispose(); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
