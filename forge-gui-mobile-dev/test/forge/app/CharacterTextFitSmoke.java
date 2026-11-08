package forge.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Clipboard;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Timer;
import forge.Forge;
import forge.adventure.campaign.CampaignRegistry;
import forge.adventure.scene.NewGameScene;
import forge.adventure.scene.StartScene;
import forge.adventure.util.Controls;
import forge.adventure.util.Selector;

import java.nio.file.Files;
import java.nio.file.Path;

/** Real font/skin clipping checks; never opens a visible window or uses a normal save profile. */
public final class CharacterTextFitSmoke {
    private final Path output = Path.of(System.getProperty("textfit.output"));
    private final long started = System.nanoTime();
    private NewGameScene creator;
    private int phase, settled;
    private long frame = -1;

    public static void main(String[] args) {
        if (!Boolean.getBoolean("textfit.isolatedProfile")) throw new IllegalStateException("Disposable profile required");
        CharacterTextFitSmoke probe = new CharacterTextFitSmoke();
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Character text fitting check"); config.setWindowedMode(1920, 1080);
        config.setInitialVisible(false); config.disableAudio(true);
        config.setForegroundFPS(30); config.setIdleFPS(15); config.setPauseWhenLostFocus(false);
        config.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override public void created(Lwjgl3Window window) {
                Timer.schedule(new Timer.Task() { @Override public void run() { probe.tick(); } }, 1, .25f);
            }
        });
        String assets = Path.of(System.getProperty("textfit.source"), "forge-gui").toString().replace('\\', '/') + "/";
        var app = Forge.getApp(null, new Lwjgl3Clipboard(), new Main.DesktopAdapter(probe.output.resolve("orientation.ini").toString()) {
            @Override public String getVersionString() { return "2.0.16-GITCharacterCreation"; }
            @Override public boolean isConnectedToInternet() { return false; }
            @Override public boolean isConnectedToWifi() { return false; }
            @Override public void setMouseConfined(boolean confined) { }
            @Override public void preventSystemSleep(boolean prevent) { }
        }, assets, false, false, 0);
        Forge.selector = "Adventure"; new Lwjgl3Application(app, config);
    }

    private void tick() {
        try {
            require((System.nanoTime() - started) / 1_000_000_000 < 180, "Text-fit check timed out");
            Gdx.graphics.requestRendering();
            if (Forge.getTransitionScreen() != null || frame == Gdx.graphics.getFrameId()) return;
            frame = Gdx.graphics.getFrameId();
            if (phase == 0) {
                if (!(Forge.getCurrentScene() instanceof StartScene)) {
                    if (Forge.afterDBloaded && Forge.getSplashScreen() != null && Forge.getSplashScreen().isShowModeSelector()) {
                        Forge.clearSplashScreen(); Forge.switchToAdventure();
                    }
                    return;
                }
                Forge.clearSplashScreen(); Forge.clearCurrentScreen(); Forge.startContinuousRendering();
                creator = NewGameScene.forCampaign(CampaignRegistry.discover().findByPlane("CurseOfStrahd"));
                Forge.switchScene(creator); phase = 1; return;
            }
            if (++settled < (phase == 3 ? 9 : 3)) return;
            settled = 0;
            if (phase == 1) {
                check("gender", "race", "portraits", "difficulty");
                for (String name : new String[]{"gender", "race", "portraits", "difficulty"}) {
                    Selector selector = (Selector) creator.getUI().getStage().getRoot().findActor(name);
                    int original = selector.getCurrentIndex();
                    for (int index = 0; index < selector.getTextList().size; index++) {
                        selector.setCurrentIndex(index); check(name);
                    }
                    selector.setCurrentIndex(original);
                }
                Selector portrait = (Selector) creator.getUI().getStage().getRoot().findActor("portraits");
                portrait.getLabel().setText("A campaign portrait with a very long descriptive name");
                portrait.getLabel().validate(); check("portraits");
                require(((Controls.MarqueeButton) portrait.getLabel()).getClippedLength() > 0, "Long names must still scroll horizontally");
                portrait.setCurrentIndex(0);
                Selector gender = (Selector) creator.getUI().getStage().getRoot().findActor("gender"); gender.setCurrentIndex(2);
                Selector race = (Selector) creator.getUI().getStage().getRoot().findActor("race");
                for (int i = 0; i < race.getTextList().size; i++) if ("Elf".equals(race.getTextList().get(i))) race.setCurrentIndex(i);
                phase = 2; return;
            }
            if (phase == 2) {
                capture("creator"); check("gender", "race", "portraits", "difficulty");
                var open = NewGameScene.class.getDeclaredMethod("showDeckWindow"); open.setAccessible(true); open.invoke(creator);
                phase = 3; return;
            }
            if (phase == 3) {
                capture("class-window"); check("starterDeck", "cardChoice");
                Files.writeString(output.resolve("PASS"), "CHARACTER_TEXT_FIT_PASS\n");
                System.out.println("CHARACTER_TEXT_FIT_PASS"); System.exit(0);
            }
        } catch (Throwable error) { error.printStackTrace(); System.exit(1); }
    }

    private void check(String... names) throws Exception {
        StringBuilder issues = new StringBuilder(), measurements = new StringBuilder();
        for (String name : names) {
            Actor actor = creator.getUI().getStage().getRoot().findActor(name);
            require(actor instanceof Selector, "Missing selector " + name);
            Selector selector = (Selector) actor;
            var button = selector.getLabel(); button.validate();
            var text = button.getTextraLabel();
            float available = button.getHeight() - button.getPadTop() - button.getPadBottom();
            measurements.append(name).append(": row=").append(button.getHeight()).append(" top=").append(button.getPadTop())
                    .append(" bottom=").append(button.getPadBottom()).append(" text=").append(text.getPrefHeight())
                    .append(" labelY=").append(text.getY()).append(" labelHeight=").append(text.getHeight()).append('\n');
            if (text.getPrefHeight() > available + .01f) issues.append(name).append(" clips text vertically; ");
            if (button instanceof Controls.MarqueeButton && text.getPrefWidth() <= button.getWidth() - button.getPadLeft() - button.getPadRight()
                    && ((Controls.MarqueeButton) button).getClippedLength() > .01f) issues.append(name).append(" scrolls text that fits; ");
        }
        Files.writeString(output.resolve("measurements-" + phase + ".txt"), measurements);
        System.out.print(measurements);
        require(issues.length() == 0, issues.toString());
    }

    private void capture(String name) {
        Pixmap pixels = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try { PixmapIO.writePNG(Gdx.files.absolute(output.resolve(name + ".png").toString()), pixels, 6, true); }
        finally { pixels.dispose(); }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
