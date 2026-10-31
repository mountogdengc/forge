package forge.app;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Clipboard;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import forge.Forge;
import forge.adventure.campaign.CampaignDefinition;
import forge.adventure.campaign.CampaignManager;
import forge.adventure.campaign.CampaignRegistry;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.scene.GameScene;
import forge.adventure.scene.StartScene;
import forge.adventure.scene.TileMapScene;
import forge.adventure.scene.QuestLogScene;
import forge.adventure.scene.NewGameScene;
import forge.adventure.campaign.CampaignContentValidator;
import forge.adventure.stage.MapStage;
import forge.adventure.util.AdventureModes;
import forge.adventure.util.Config;
import forge.adventure.util.Current;
import forge.adventure.world.ExplorationSnapshot;
import forge.adventure.world.SaveKind;
import forge.adventure.world.SaveSnapshot;
import forge.adventure.world.WorldSave;
import forge.adventure.world.PlaythroughRepository;
import forge.adventure.world.WorldSaveHeader;
import forge.adventure.util.SaveFileData;
import forge.localinstance.properties.ForgeConstants;
import forge.card.ColorSet;

/** Explicit graphical smoke runner; always launch with a disposable profile (see Bellwake validation notes). */
public final class InteriorRestorationSmoke {
    private int phase;
    private int frames;
    private long lastFrame = -1;
    private final long started = System.nanoTime();
    private SaveSnapshot saved;
    private ExplorationSnapshot captured;
    private int savedLife;
    private int savedGold;
    private BellwakeStorySmoke story;
    private InteriorFixtureSmoke interiorFixture;

    public static void main(String[] args) {
        if (!Boolean.getBoolean("bellwake.isolatedProfile")) throw new IllegalStateException("An isolated profile is required.");
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Bellwake interior restoration smoke");
        config.setWindowedMode(Integer.getInteger("bellwake.width", 1280), Integer.getInteger("bellwake.height", 720));
        config.setInitialVisible(true);
        config.setForegroundFPS(60);
        config.disableAudio(true);
        InteriorRestorationSmoke smoke = new InteriorRestorationSmoke();
        config.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override public void created(Lwjgl3Window window) {
                Timer.schedule(new Timer.Task() {
                    @Override public void run() { smoke.tick(); }
                }, 1f, 0.25f);
            }
        });
        ApplicationListener game = Forge.getApp(null, new Lwjgl3Clipboard(), new Main.DesktopAdapter("../forge-gui/switch_orientation.ini") {
            @Override public String getVersionString() { return "2.0.16-GITBellwakeSmoke"; }
        }, "../forge-gui/", Integer.getInteger("bellwake.height", 720) > Integer.getInteger("bellwake.width", 1280), false, 0);
        Forge.selector = "Adventure";
        new Lwjgl3Application(game, config);
    }

    private void tick() {
        try {
            Gdx.graphics.requestRendering();
            if ((System.nanoTime() - started) / 1_000_000_000L > 180) throw new AssertionError("Graphical smoke timed out at phase " + phase);
            // Timer callbacks can accumulate during a resource load and run in one frame.
            // Let the real application render/act between interaction steps.
            long frame = Gdx.graphics.getFrameId();
            if (frame == lastFrame) return;
            lastFrame = frame;
            if (phase == 0) {
                if (Forge.getCurrentScene() instanceof StartScene && Forge.getTransitionScreen() == null) {
                    Forge.clearSplashScreen();
                    Forge.clearCurrentScreen();
                    Forge.startContinuousRendering();
                    phase = 1;
                } else if (Forge.afterDBloaded && Forge.getSplashScreen().isShowModeSelector()) {
                    Forge.clearSplashScreen();
                    Forge.switchToAdventure();
                }
                return;
            }
            if (phase == 10 && Forge.getCurrentScene() instanceof TileMapScene)
                require(Forge.getTransitionScreen() == null, "Interior Continue left its loading overlay open");
            if (++frames < 2 || Forge.getTransitionScreen() != null) return;
            frames = 0;
            if (phase == 1) {
                if (Boolean.getBoolean("bellwake.resume")) {
                    require(StartScene.instance().Continue(), "Continue action did not start");
                    phase = 10;
                    return;
                }
                CampaignDefinition campaign = CampaignRegistry.discover().getCampaigns().stream()
                        .filter(c -> c.getId().equals("fork.bellwake")).findFirst().orElseThrow();
                var diagnostics = CampaignContentValidator.validate(java.nio.file.Path.of(Config.instance().getAdventureRoot(), "Bellwake"),
                        java.nio.file.Path.of(Config.instance().getAdventureRoot(), "common"),
                        name -> forge.StaticData.instance().getCommonCards().getCard(name) != null);
                require(diagnostics.isEmpty(), diagnostics.toString());
                NewGameScene preview = NewGameScene.forCampaign(campaign);
                var modesField = NewGameScene.class.getDeclaredField("modes");
                modesField.setAccessible(true);
                var modes = (com.badlogic.gdx.utils.Array<?>) modesField.get(preview);
                require(modes.size == 1 && modes.first() == AdventureModes.Constructed, "Unexpected inherited starter modes");
                CampaignManager.activate(campaign);
                WorldSave.beginPlaythrough(campaign, "Interior smoke", "Smoke");
                Config config = Config.instance();
                WorldSave.generateNewWorld("Smoke", true, 0, 0, ColorSet.fromNames("W"),
                        config.getConfigData().difficulties[0], AdventureModes.Constructed, 0, null, 0);
                require(WorldSave.saveInitialSnapshot(), WorldSave.getLastError());
                Forge.clearTransitionScreen();
                Forge.switchScene(GameScene.instance());
                enter("Spawn");
                phase = 2;
            } else if (phase == 2) {
                Current.player().heal(-3);
                savedLife = Current.player().getLife();
                savedGold = Current.player().getGold();
                captured = WorldSave.captureExploration();
                require(WorldSave.saveManual("Quay interior"), WorldSave.getLastError());
                saved = WorldSave.listSnapshots(WorldSave.getCurrentPlaythrough().playthroughId).stream()
                        .filter(s -> s.kind == SaveKind.MANUAL).findFirst().orElseThrow();
                Current.player().giveGold(55);
                require(WorldSave.loadSnapshot(saved), WorldSave.getLastError());
                WorldSave.showRestoredExploration();
                phase = 3;
            } else if (phase == 3) {
                require(Forge.getCurrentScene() instanceof TileMapScene, "Interior scene not restored");
                require(Current.player().getLife() == savedLife, "Restoration healed the player");
                require(Current.player().getGold() == savedGold, "Inventory state not restored");
                ExplorationSnapshot actual = WorldSave.captureExploration();
                require(actual.poiId.equals(captured.poiId), "Wrong POI restored");
                require(actual.playerX == captured.playerX && actual.playerY == captured.playerY, "Wrong interior position");
                MapStage.getInstance().startPause(0.01f);
                MapStage.getInstance().openMenu();
                require(Forge.getCurrentScene() instanceof TileMapScene, "Opening the menu froze a settling exploration action");
                MapStage.getInstance().startPause(0f);
                Forge.getCurrentScene().render();
                Pixmap shot = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
                try {
                    PixmapIO.writePNG(Gdx.files.absolute(System.getProperty("bellwake.screenshot")), shot, 6, true);
                } finally { shot.dispose(); }
                enter("Bellwake Tollhouse");
                phase = 4;
            } else if (phase == 4) {
                require(MapStage.getInstance().lookForID(60), "Keeper fixture missing");
                float worldX = Current.player().getWorldPosX();
                float worldY = Current.player().getWorldPosY();
                MapStage.getInstance().keyDown(Input.Keys.F5);
                require(Current.player().getWorldPosX() == worldX && Current.player().getWorldPosY() == worldY,
                        "Interior F5 overwrote the overworld return position");
                MapStage.getInstance().deleteObject(60);
                require(WorldSave.getCurrentSave().quickLoad(), WorldSave.getLastError());
                WorldSave.showRestoredExploration();
                phase = 5;
            } else if (phase == 5) {
                require(MapStage.getInstance().lookForID(60), "Loading pre-victory checkpoint lost the Keeper");
                require(TileMapScene.instance().rootPoint.getData().name.equals("Bellwake Tollhouse"), "Wrong return location");
                if (Boolean.getBoolean("bellwake.victoryProbe")) {
                    BellwakeStorySmoke.victory(60);
                    System.out.println("BELLWAKE_VICTORY_GUARD_PASS");
                    Gdx.app.exit();
                    phase = 8;
                    return;
                }
                verifyFailedCrossCampaignLoad();
                WorldSave.showRestoredExploration();
                phase = 6;
            } else if (phase == 6) {
                require(MapStage.getInstance().lookForID(60), "Rollback lost the Keeper");
                interiorFixture = new InteriorFixtureSmoke();
                interiorFixture.begin();
                phase = 9;
            } else if (phase == 9 && interiorFixture.tick()) {
                Current.player().setQuestFlag("bellwakeResolved", 1);
                requireCompletionJournal();
                story = new BellwakeStorySmoke();
                story.begin();
                phase = 7;
            } else if (phase == 7 && story.tick()) {
                Current.player().heal(-3); // A fresh interior Continue must not replay town healing.
                require(WorldSave.saveManual("Fresh-process Continue"), WorldSave.getLastError());
                java.util.Properties expected = new java.util.Properties();
                ExplorationSnapshot location = WorldSave.captureExploration();
                expected.setProperty("x", Float.toString(location.playerX));
                expected.setProperty("y", Float.toString(location.playerY));
                expected.setProperty("life", Integer.toString(Current.player().getLife()));
                expected.setProperty("gold", Integer.toString(Current.player().getGold()));
                expected.setProperty("poi", location.poiId);
                try (var output = java.nio.file.Files.newOutputStream(java.nio.file.Path.of(System.getProperty("bellwake.expected")))) {
                    expected.store(output, "Fresh-process restoration expectations");
                }
                requireCompletionJournal();
                System.out.println("BELLWAKE_INTERIOR_SMOKE_PASS: interior saves, F5 return position, failed cross-campaign rollback, curated starter modes, actual card references, completed journal");
                System.out.println("BELLWAKE_ASSISTED_STORY_PASS: practice reward once, both endings, reload/revisit rewards and defeated Keeper, no partial ending saves");
                Gdx.app.exit();
                phase = 8;
            } else if (phase == 10) {
                java.util.Properties expected = new java.util.Properties();
                try (var input = java.nio.file.Files.newInputStream(java.nio.file.Path.of(System.getProperty("bellwake.expected")))) {
                    expected.load(input);
                }
                require(Forge.getCurrentScene() instanceof TileMapScene, "Fresh Continue did not restore an interior");
                ExplorationSnapshot location = WorldSave.captureExploration();
                require(location.poiId.equals(expected.getProperty("poi")), "Fresh Continue changed POI");
                require(location.playerX == Float.parseFloat(expected.getProperty("x")) && location.playerY == Float.parseFloat(expected.getProperty("y")), "Fresh Continue changed position");
                require(Current.player().getLife() == Integer.parseInt(expected.getProperty("life")), "Fresh Continue healed the player");
                require(Current.player().getGold() == Integer.parseInt(expected.getProperty("gold")), "Fresh Continue changed currency");
                require(Current.player().getCharacterFlag("bellwakeEnding") == 2 && Current.player().checkQuestFlag("bellwakeResolved"), "Fresh Continue lost the ending");
                require(Current.player().getCards().toFlatList().stream().filter(c -> c.getName().equals("Traveler's Amulet")).count() == 1, "Fresh Continue lost or repeated the reward");
                require(Current.player().getSelectedDeck().getMain().countAll() == 40, "Fresh Continue changed the deck");
                enter("Bellwake Tollhouse");
                require(!MapStage.getInstance().lookForID(60), "Fresh Continue resurrected the Keeper");
                SaveSnapshot source = WorldSave.listSnapshots(WorldSave.getCurrentPlaythrough().playthroughId).stream()
                        .filter(s -> s.label.equals("Fresh-process Continue")).findFirst().orElseThrow();
                // Bellwake hides NG+; this probes the shared API's context reset for other campaigns.
                WorldSave.beginNewGamePlus("Interior context reset probe");
                require(!WorldSave.captureExploration().interior, "NG+ retained the previous world's interior");
                require(WorldSave.loadSnapshot(source), WorldSave.getLastError());
                WorldSave.showRestoredExploration();
                System.out.println("BELLWAKE_FRESH_CONTINUE_PASS");
                Gdx.app.exit();
                phase = 8;
            }
        } catch (Throwable error) {
            error.printStackTrace();
            System.err.println("BELLWAKE_INTERIOR_SMOKE_FAIL phase=" + phase);
            System.exit(1);
        }
    }

    private static void enter(String name) {
        PointOfInterest point = Current.world().getAllPointOfInterest().stream()
                .filter(p -> p.getData().name.equals(name)).findFirst().orElseThrow();
        TileMapScene.instance().load(point);
        Forge.switchScene(TileMapScene.instance());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireCompletionJournal() throws Exception {
        QuestLogScene journal = QuestLogScene.instance(Forge.getCurrentScene());
        Forge.switchScene(journal);
        var field = QuestLogScene.class.getDeclaredField("scrollContainer");
        field.setAccessible(true);
        var table = (com.badlogic.gdx.scenes.scene2d.ui.Table) field.get(journal);
        boolean completed = false;
        for (var actor : table.getChildren()) completed |= "chapter-complete".equals(actor.getName());
        require(completed, "Completed chapter disappeared from the journal");
    }

    private static void verifyFailedCrossCampaignLoad() throws Exception {
        java.nio.file.Path source = java.nio.file.Path.of(Config.instance().getPrefix()).toAbsolutePath();
        java.nio.file.Path fixture = java.nio.file.Path.of(ForgeConstants.USER_ADVENTURE_DIR, "userplanes", "SmokeFailure");
        try (var paths = java.nio.file.Files.walk(source)) {
            for (java.nio.file.Path from : (Iterable<java.nio.file.Path>) paths::iterator) {
                java.nio.file.Path to = fixture.resolve(source.relativize(from));
                if (java.nio.file.Files.isDirectory(from)) java.nio.file.Files.createDirectories(to);
                else java.nio.file.Files.copy(from, to, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
        java.nio.file.Path manifest = fixture.resolve("campaign.json");
        java.nio.file.Files.writeString(manifest, java.nio.file.Files.readString(manifest).replace("fork.bellwake", "fixture.bellwake.failure"));
        CampaignDefinition campaign = CampaignRegistry.discover().getCampaigns().stream()
                .filter(c -> c.getId().equals("fixture.bellwake.failure")).findFirst().orElseThrow();
        PlaythroughRepository repository = new PlaythroughRepository(java.nio.file.Path.of(ForgeConstants.USER_ADVENTURE_DIR, "campaign-saves"));
        var priorRun = WorldSave.getCurrentPlaythrough();
        SaveSnapshot priorContinue = repository.getContinue();
        SaveSnapshot good = WorldSave.listSnapshots(priorRun.playthroughId).stream().filter(s -> s.kind == SaveKind.QUICK).findFirst().orElseThrow();
        WorldSaveHeader header;
        SaveFileData data;
        try (var input = new java.io.ObjectInputStream(new java.util.zip.InflaterInputStream(java.nio.file.Files.newInputStream(repository.validatedPayload(good))))) {
            header = (WorldSaveHeader) input.readObject();
            data = (SaveFileData) input.readObject();
        }
        ExplorationSnapshot badLocation = ExplorationSnapshot.fromSaveData(data.readSubData("exploration"));
        badLocation.map = "maps/map/bellwake/missing-smoke-fixture.tmx";
        data.store("exploration", badLocation.toSaveData());
        var run = repository.createPlaythrough(campaign.getId(), campaign.getVersion(), campaign.getTitle(), campaign.getPlane(), "Failure fixture", "Smoke");
        SaveSnapshot broken;
        try {
            broken = repository.writeSnapshot(repository.prepareSave(run, SaveKind.MANUAL, "Missing map", "Missing", 2), path -> {
                try (var output = new java.io.ObjectOutputStream(new java.util.zip.DeflaterOutputStream(java.nio.file.Files.newOutputStream(path)))) {
                    output.writeObject(header);
                    output.writeObject(data);
                }
            }, path -> { }); // Intentionally valid bytes referencing an unavailable map.
        } finally { header.dispose(); }
        ExplorationSnapshot priorPosition = WorldSave.captureExploration();
        require(!WorldSave.loadSnapshot(broken), "Missing map unexpectedly loaded");
        require(WorldSave.getCurrentPlaythrough().playthroughId.equals(priorRun.playthroughId), "Failed load changed the active run");
        require(repository.getContinue().snapshotId.equals(priorContinue.snapshotId), "Failed load changed Continue");
        ExplorationSnapshot restored = WorldSave.captureExploration();
        require(restored.poiId.equals(priorPosition.poiId) && restored.playerX == priorPosition.playerX && restored.playerY == priorPosition.playerY,
                "Failed load did not restore the outgoing interior");
    }
}
