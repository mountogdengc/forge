package forge.app;

import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import forge.Forge;
import forge.adventure.campaign.CampaignContentValidator;
import forge.adventure.campaign.CampaignDefinition;
import forge.adventure.campaign.CampaignManager;
import forge.adventure.campaign.CampaignRegistry;
import forge.adventure.character.DialogActor;
import forge.adventure.scene.GameScene;
import forge.adventure.scene.RewardScene;
import forge.adventure.scene.StartScene;
import forge.adventure.scene.TileMapScene;
import forge.adventure.stage.MapStage;
import forge.adventure.util.AdventureModes;
import forge.adventure.util.AdventureQuestController;
import forge.adventure.util.Config;
import forge.adventure.util.Current;
import forge.adventure.world.SaveKind;
import forge.adventure.world.SaveSnapshot;
import forge.adventure.world.WorldSave;
import forge.card.ColorSet;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.nio.file.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;

/** Real hidden libGDX interactions, with explicitly supplied duel outcomes. */
final class StrahdChapterSmoke {
    @FunctionalInterface private interface Step { boolean run() throws Exception; }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private final List<Step> steps = new ArrayList<>();
    private int index;
    private SaveSnapshot church, finished;
    private int practiceCards, life, gold;
    private long reminderReady;
    private final String starter = System.getProperty("bellwake.starter", "W");
    private final String upgrade = starter.equals("B") ? "Murder" : "Rebuke";

    void begin() throws Exception {
        if (Boolean.getBoolean("bellwake.packagedProbe")) {
            require(Path.of(forge.localinstance.properties.ForgeConstants.USER_DIR).toAbsolutePath().normalize().equals(
                    Path.of("test-profile").toAbsolutePath().normalize()), "Portable test did not use packaged profile");
            require(CampaignManager.current().getId().equals("fork.curse-of-strahd"), "Portable build did not select Strahd");
            System.out.println("STRAHD_PORTABLE_START_PASS");
        }
        if (Boolean.getBoolean("strahd.resume")) {
            require(StartScene.instance().Continue(), "Strahd Continue failed");
            stable();
            action(() -> {
                require(CampaignManager.current().getId().equals("fork.curse-of-strahd"), "Wrong Continue campaign");
                require(flag("cosChapterOneComplete") && Current.player().getCharacterFlag("cosDoruFate") == 2,
                        "Fresh Continue lost chapter outcome");
                require(TileMapScene.instance().rootPoint.getData().name.equals("Tser Pool"), "Continue lost camp interior");
                require(owned(upgrade) == 2 + starterUpgradeCount(), "Continue lost or repeated practice reward");
                Properties expected = new Properties();
                try (var stream = Files.newInputStream(Path.of(System.getProperty("bellwake.expected")))) { expected.load(stream); }
                var location = WorldSave.captureExploration();
                require(location.poiId.equals(expected.getProperty("poi")) &&
                        location.playerX == Float.parseFloat(expected.getProperty("x")) &&
                        location.playerY == Float.parseFloat(expected.getProperty("y")), "Continue changed exact camp location");
                require(Current.player().getLife() == Integer.parseInt(expected.getProperty("life")) &&
                        Current.player().getGold() == Integer.parseInt(expected.getProperty("gold")), "Continue changed life/currency");
                System.out.println("STRAHD_FRESH_CONTINUE_PASS");
            });
            action(() -> {
                enter("Old Svalich Road");
                require(!MapStage.getInstance().lookForID(60), "Completed-save wolves reappeared");
                if (flag("cosStrahdDeparted")) require(!MapStage.getInstance().lookForID(62), "Departed Strahd reappeared");
                enter("Village Church");
                require(!MapStage.getInstance().lookForID(60), "Completed-save Doru reappeared");
                enter("Tser Pool");
                System.out.println("STRAHD_COMPLETED_ACTOR_RECONCILIATION_PASS");
            });
            return;
        }
        CampaignDefinition campaign = find("fork.curse-of-strahd");
        Path root = Path.of(Config.instance().getAdventureRoot());
        var errors = CampaignContentValidator.validate(root.resolve("CurseOfStrahd"), root.resolve("common"),
                name -> forge.StaticData.instance().getCommonCards().getCard(name) != null);
        require(errors.isEmpty(), errors.toString());
        require(WorldSave.checkpointAndSuspend(), WorldSave.getLastError());
        start(campaign, starter);
        var quest = AdventureQuestController.instance().generateQuest(8101);
        quest.prologue = null;
        Current.player().addQuest(quest, true);
        require(WorldSave.saveInitialSnapshot(), WorldSave.getLastError());
        enter("Spawn");
        practiceCards = owned(upgrade);
        require(practiceCards == starterUpgradeCount(), "Unexpected starter upgrade count");
        action(() -> {
            var hud = forge.adventure.stage.GameHUD.getInstance();
            notificationPane().clearActions();
            var stage = quest.getActiveStages().get(0);
            hud.addNotification("[!]" + quest.name + "[]\n" + stage.name + "\n[/]" + stage.description + "[]");
            reminderReady = System.nanoTime() + 3_000_000_000L;
        });
        steps.add(() -> System.nanoTime() >= reminderReady);
        action(() -> {
            var pane = notificationPane();
            require(pane.getY() >= 0 && pane.getY() + pane.getHeight() <= forge.adventure.stage.GameHUD.getInstance().getHeight(),
                    "Settled quest reminder exceeds HUD bounds");
        });
        action(() -> { screenshot("village"); life = Current.player().getLife(); gold = Current.player().getGold(); outcome(62, false); });
        stable();
        action(() -> {
            require(MapStage.getInstance().isInMap() && MapStage.getInstance().lookForID(62), "Practice loss ejected player");
            require(Current.player().getLife() == life && Current.player().getGold() == gold, "Practice loss applied penalty");
            BellwakeStorySmoke.victory(62);
        });
        reward(); click(starter.equals("B") ? 1 : 0); reward(); click(0); stable();
        action(() -> {
            require(owned(upgrade) == practiceCards + 2 && flag("cosTrainingRewarded"), "Practice upgrade missing");
            require(Current.player().getSelectedDeck().getMain().countAll() == 40, "Reward changed selected deck");
            BellwakeStorySmoke.victory(62);
        });
        reward(); click(0); stable();
        action(() -> require(owned(upgrade) == practiceCards + 2, "Practice reward repeated"));
        action(() -> enter("Tser Pool")); talk(60); click(0); stable();
        action(() -> { screenshot("tser-pool"); require(!flag("cosChapterOneComplete"), "Early camp visit completed chapter"); });
        action(() -> enter("Old Svalich Road")); talk(60); click(0); stable();
        action(() -> { screenshot("road"); require(!flag("cosRoadCleared"), "Early road visit cleared encounter"); });
        action(() -> enter("Village Church")); stable();
        action(() -> outcome(60, false)); stable();
        action(() -> {
            require(!MapStage.getInstance().isInMap() && Forge.getCurrentScene() instanceof GameScene,
                    "A church loss did not return to overworld exploration");
            require(!flag("cosDoruSubdued"), "A church loss advanced the story");
            enter("Village Church");
        }); stable();
        action(() -> { screenshot("church"); require(WorldSave.saveManual("Before Doru"), WorldSave.getLastError()); church = manual("Before Doru"); });
        branch(1);
        action(() -> {
            require(WorldSave.saveManual("Restrained completion"), WorldSave.getLastError());
            finished = manual("Restrained completion");
            require(WorldSave.checkpointAndSuspend(), WorldSave.getLastError());
            start(find("fork.bellwake"), "W");
            require(!flag("cosChapterOneComplete") && Current.player().getCharacterFlag("cosDoruFate") == 0,
                    "Strahd progress leaked into Bellwake");
            require(WorldSave.loadSnapshot(finished), WorldSave.getLastError());
            WorldSave.showRestoredExploration();
        }); stable();
        action(() -> {
            require(flag("cosChapterOneComplete") && Current.player().getCharacterFlag("cosDoruFate") == 1,
                    "Cross-campaign restore lost Strahd state");
            require(WorldSave.loadSnapshot(church), WorldSave.getLastError());
            WorldSave.showRestoredExploration();
        }); stable();
        action(() -> require(!flag("cosBriefed") && !flag("cosChurchResolved"), "Earlier checkpoint retained future state"));
        branch(2);
        action(() -> {
            Current.player().heal(-3);
            require(WorldSave.saveManual("Strahd fresh Continue"), WorldSave.getLastError());
            Properties expected = new Properties();
            var location = WorldSave.captureExploration();
            expected.setProperty("poi", location.poiId);
            expected.setProperty("x", Float.toString(location.playerX));
            expected.setProperty("y", Float.toString(location.playerY));
            expected.setProperty("life", Integer.toString(Current.player().getLife()));
            expected.setProperty("gold", Integer.toString(Current.player().getGold()));
            try (var stream = Files.newOutputStream(Path.of(System.getProperty("bellwake.expected")))) { expected.store(stream, "Strahd assisted expectations"); }
            System.out.println("STRAHD_STORY_CHECKS_PASS: both fates, late briefing, early camp, friendly loss, reward once, checkpoint and campaign isolation");
        });
    }

    private void branch(int fate) {
        action(() -> BellwakeStorySmoke.victory(60)); reward(); click(fate - 1); click(0); stable();
        action(() -> {
            require(flag("cosDoruSubdued") && flag("cosChurchResolved"), "Church did not resolve");
            require(!MapStage.getInstance().lookForID(60), "Resolved Doru remained after his final line");
            require(Current.player().getCharacterFlag("cosDoruFate") == fate, "Wrong Doru fate");
            require(WorldSave.saveManual("Church fate " + fate), WorldSave.getLastError());
            require(WorldSave.loadSnapshot(manual("Church fate " + fate)), WorldSave.getLastError());
            WorldSave.showRestoredExploration();
        }); stable();
        action(() -> require(!MapStage.getInstance().lookForID(60), "Saved resolved Doru reappeared"));
        talk(61); click(0); stable();
        action(() -> enter("Spawn")); talk(60); click(0); click(0); stable();
        action(() -> require(flag("cosBriefed"), "Late briefing did not commit"));
        talk(61); click(0); click(0); stable();
        action(() -> require(flag("cosEscortAccepted"), "Escort not accepted"));
        action(() -> enter("Tser Pool")); talk(60); click(0); stable();
        action(() -> require(!flag("cosChapterOneComplete"), "Camp completed before pack defeat"));
        action(() -> enter("Old Svalich Road"));
        talk(62);
        action(() -> {
            var stage = MapStage.getInstance();
            stage.getPlayerSprite().setPosition(440f, 350f);
            // The fixture teleports rather than walks; set the camera before departure pauses input.
            stage.getCamera().position.set(400f, 350f, 0f);
            stage.getCamera().update();
        });
        click(0);
        action(() -> {
            require(flag("cosStrahdDeparted") && !MapStage.getInstance().lookForID(62), "Strahd departure did not commit");
            require(MapStage.getInstance().getSpriteGroup().findActor("departing-map-object-62") != null, "Mounted exit visual missing");
            require(!WorldSave.saveManual("Unsafe departure"), "Saved halfway through Strahd's exit");
            screenshot("strahd-riding");
        }); stable();
        action(() -> {
            require(MapStage.getInstance().getSpriteGroup().findActor("departing-map-object-62") == null, "Strahd's exit visual remained");
            enter("Old Svalich Road");
            require(!MapStage.getInstance().lookForID(62), "Strahd returned on revisit");
            BellwakeStorySmoke.victory(60);
        }); reward(); stable();
        action(() -> {
            require(flag("cosRoadCleared") && !MapStage.getInstance().lookForID(60), "Defeated pack remained on screen");
            enter("Old Svalich Road");
            require(!MapStage.getInstance().lookForID(60), "Defeated pack reappeared");
            enter("Tser Pool");
        });
        talk(61); click(0); stable();
        action(() -> require(!flag("cosChapterOneComplete"), "Arrival completed chapter without Eva"));
        talk(60); click(0); click(0); stable();
        action(() -> require(flag("cosChapterOneComplete"), "Eva did not complete chapter"));
        talk(60); click(0); stable();
        action(() -> require(owned(upgrade) == practiceCards + 2, "Chapter revisit repeated upgrade"));
    }

    boolean tick() throws Exception {
        if (index == steps.size()) return true;
        System.out.println("STRAHD_SMOKE step=" + index + " policy=" + WorldSave.explorationSaveBlockReason());
        if (steps.get(index).run()) index++;
        return false;
    }
    private void action(Action action) { steps.add(() -> { action.run(); return true; }); }
    private void stable() { steps.add(() -> WorldSave.explorationSaveBlockReason().isEmpty()); }
    private void reward() { steps.add(() -> { if (!(Forge.getCurrentScene() instanceof RewardScene)) return false;
        BellwakeStorySmoke.finishRewards(); return true; }); }
    private void talk(int id) { action(() -> {
        var actor = MapStage.getInstance().getByID(id);
        if (actor instanceof DialogActor dialog) dialog.onPlayerCollide();
        else MapStage.getInstance().getEnemyByID(id).dialog.activate();
    }); }
    private void click(int choice) { action(() -> {
        var buttons = MapStage.getInstance().getDialog().getButtonTable().getChildren();
        require(buttons.size > choice, "Dialogue choice " + choice + " missing at step " + index);
        for (var listener : new com.badlogic.gdx.utils.Array<>(buttons.get(choice).getListeners()))
            if (listener instanceof ClickListener click) click.clicked(null, 0, 0);
    }); }
    private static CampaignDefinition find(String id) {
        return CampaignRegistry.discover().getCampaigns().stream().filter(c -> c.getId().equals(id)).findFirst().orElseThrow();
    }
    private static void start(CampaignDefinition campaign, String color) {
        CampaignManager.activate(campaign);
        WorldSave.beginPlaythrough(campaign, "Assisted chapter check", "Chapter Check");
        WorldSave.generateNewWorld("Chapter Check", true, 0, 0, ColorSet.fromNames(color),
                Config.instance().getConfigData().difficulties[0], AdventureModes.Constructed, 0, null, 0);
        Forge.switchScene(GameScene.instance());
    }
    private static void enter(String name) {
        var point = Current.world().getAllPointOfInterest().stream().filter(p -> p.getData().name.equals(name)).findFirst().orElseThrow();
        TileMapScene.instance().load(point); Forge.switchScene(TileMapScene.instance());
    }
    private static SaveSnapshot manual(String label) {
        return WorldSave.listSnapshots(WorldSave.getCurrentPlaythrough().playthroughId).stream()
                .filter(s -> s.kind == SaveKind.MANUAL && s.label.equals(label)).findFirst().orElseThrow();
    }
    private static void outcome(int id, boolean win) throws Exception {
        var field = MapStage.class.getDeclaredField("currentMob"); field.setAccessible(true);
        field.set(MapStage.getInstance(), MapStage.getInstance().getEnemyByID(id));
        MapStage.getInstance().setWinner(win, false);
    }
    private static boolean flag(String name) { return Current.player().checkQuestFlag(name); }
    private static com.badlogic.gdx.scenes.scene2d.Actor notificationPane() throws Exception {
        var field = forge.adventure.stage.GameHUD.class.getDeclaredField("notificationPane");
        field.setAccessible(true);
        return (com.badlogic.gdx.scenes.scene2d.Actor) field.get(forge.adventure.stage.GameHUD.getInstance());
    }
    private static void screenshot(String name) {
        Pixmap shot = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try {
            Path path = Path.of(System.getProperty("bellwake.screenshot")).getParent().resolve("strahd-" + name + ".png");
            PixmapIO.writePNG(Gdx.files.absolute(path.toString()), shot, 6, true);
        } finally { shot.dispose(); }
    }
    private int starterUpgradeCount() { return starter.equals("B") ? 0 : 2; }
    private static int owned(String name) { return (int) Current.player().getCards().toFlatList().stream().filter(c -> c.getName().equals(name)).count(); }
    private static void require(boolean condition, String text) { if (!condition) throw new AssertionError(text); }
}
