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
    private SaveSnapshot relicCheckpoint, truthCheckpoint;
    private int practiceCards, life, gold;
    private String originalDeck;
    private int tourLife, tourGold, tourCards;
    private float landmarkReturnX, landmarkReturnY;
    private final boolean krezkMode = Boolean.getBoolean("strahd.krezk");
    private SaveSnapshot krezkWater, krezkAccord;
    private int inspectorBase, dissenterBase;
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
                require(flag("cosChapterTwoComplete") && Current.player().getCharacterFlag("cosVallakiTruth") == 2,
                        "Fresh Continue lost Vallaki outcome");
                require(TileMapScene.instance().rootPoint.getData().name.equals(krezkMode ? "Krezk Village" : "Krezk"), "Continue lost Krezk interior");
                require(owned(chapterUpgrade()) == 2, "Continue lost or repeated Vallaki reward");
                require(owned(upgrade) == 2 + starterUpgradeCount(), "Continue lost or repeated practice reward");
                Properties expected = new Properties();
                try (var stream = Files.newInputStream(Path.of(System.getProperty("bellwake.expected")))) { expected.load(stream); }
                var location = WorldSave.captureExploration();
                require(location.poiId.equals(expected.getProperty("poi")) &&
                        location.playerX == Float.parseFloat(expected.getProperty("x")) &&
                        location.playerY == Float.parseFloat(expected.getProperty("y")), "Continue changed exact camp location");
                require(Current.player().getLife() == Integer.parseInt(expected.getProperty("life")) &&
                        Current.player().getGold() == Integer.parseInt(expected.getProperty("gold")), "Continue changed life/currency");
                require(verifyGeography("continued").equals(expected.getProperty("geography")), "Continue changed authored geography");
                if (krezkMode) {
                    require(flag("cosChapterThreeComplete") && Current.player().getCharacterFlag("cosKrezkChoice") == 2,
                            "Continue lost Krezk accord/completion");
                    require(owned("Thraben Inspector") == Integer.parseInt(expected.getProperty("inspectors")) &&
                            owned("Doomed Dissenter") == Integer.parseInt(expected.getProperty("dissenters")), "Continue changed Krezk reward");
                    require(Current.player().getSelectedDeck().getMain().toCardList("\n").equals(expected.getProperty("deck")), "Continue changed selected deck");
                    System.out.println("STRAHD_KREZK_FRESH_CONTINUE_PASS");
                }
                System.out.println("STRAHD_FRESH_CONTINUE_PASS");
            });
            action(() -> {
                enter("Old Svalich Road");
                require(!MapStage.getInstance().lookForID(60), "Completed-save wolves reappeared");
                if (flag("cosStrahdDeparted")) require(!MapStage.getInstance().lookForID(62), "Departed Strahd reappeared");
                enter("Village Church");
                require(!MapStage.getInstance().lookForID(60), "Completed-save Doru reappeared");
                enter("West Svalich Road");
                require(!MapStage.getInstance().lookForID(60), "Western ambush reappeared");
                enter("Coffin Maker's Shop");
                require(!MapStage.getInstance().lookForID(61), "Coffin spawn reappeared");
                enter("Saint Andral's Church");
                require(!MapStage.getInstance().lookForID(62), "Belfry stalker reappeared");
                System.out.println("STRAHD_COMPLETED_ACTOR_RECONCILIATION_PASS");
            });
            if (krezkMode) return;
            action(() -> {
                require(WorldSave.loadSnapshot(manual("Arrival Castle Ravenloft")), WorldSave.getLastError());
                WorldSave.showRestoredExploration();
            }); stable();
            action(() -> {
                require(TileMapScene.instance().rootPoint.getData().name.equals("Castle Ravenloft"), "Fresh-process Ravenloft checkpoint lost its location");
                require(!flag("cosChapterOneComplete") && !flag("cosChapterTwoComplete"), "Early arrival checkpoint retained future chapter state");
                require(MapStage.getInstance().lookForID(60) && MapStage.getInstance().lookForID(61), "Arrival interactions lost on restore");
                System.out.println("STRAHD_LANDMARK_FRESH_RESTORE_PASS");
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
        require(Current.world().getAllPointOfInterest().size() == 37, "New world did not generate all 37 locations");
        verifyGeography("new-world");
        originalDeck = Current.player().getSelectedDeck().getMain().toCardList("\n");
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
        if (krezkMode) earlyKrezkVisits(); else tourLandmarks();
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
        action(() -> enter("West Svalich Road")); talk(60); click(0); stable();
        action(() -> enter("Vallaki")); talk(60); click(0); stable();
        action(() -> enter("Coffin Maker's Shop")); talk(60); click(0); stable();
        talk(61); click(0); stable(); talk(62); click(0); stable();
        action(() -> enter("Saint Andral's Church")); talk(60); click(0); stable();
        talk(61); click(0); stable(); talk(62); click(0); stable(); talk(63); click(0); stable();
        action(() -> require(!flag("cosWestRoadCleared") && !flag("cosBonesInvestigation") && !flag("cosChapterTwoComplete"),
                "Early western visits advanced the story"));
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
        if (krezkMode) krezkChapter();
        action(() -> prepareLandmarkVisit(krezkMode ? "Krezk Village" : "Krezk")); stable();
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
            if (krezkMode) {
                expected.setProperty("inspectors", Integer.toString(owned("Thraben Inspector")));
                expected.setProperty("dissenters", Integer.toString(owned("Doomed Dissenter")));
                expected.setProperty("deck", Current.player().getSelectedDeck().getMain().toCardList("\n"));
            }
            expected.setProperty("geography", verifyGeography("completed"));
            try (var stream = Files.newOutputStream(Path.of(System.getProperty("bellwake.expected")))) { expected.store(stream, "Strahd assisted expectations"); }
            System.out.println("STRAHD_STORY_CHECKS_PASS: two chapters, both fates/truth choices, early visits, losses, rewards once, checkpoint and campaign isolation");
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
        vallaki(fate);
    }

    private String chapterUpgrade() { return starter.equals("B") ? "Falkenrath Noble" : "Fiend Hunter"; }

    private void earlyKrezkVisits() {
        action(() -> enter("Krezk")); talk(60); click(0); stable(); talk(61); click(0); stable();
        talk(62); click(0); stable();
        action(() -> enter("Krezk Village")); talk(60); click(0); stable(); talk(62); click(0); stable();
        action(() -> enter("Blessed Pool of Krezk")); talk(61); click(0); stable(); talk(62); click(0); stable();
        action(() -> enter("Abbey of Saint Markovia")); talk(60); click(0); stable();
        talk(61); click(0); stable(); talk(62); click(0); stable(); talk(63); click(0); stable();
        action(() -> {
            require(!flag("cosKrezkQuestAccepted") && !flag("cosKrezkRoadCleared") && !flag("cosChapterThreeComplete"),
                    "Early Krezk visit advanced story");
            require(Current.player().getQuests().size() == 1, "Early Krezk visit issued a quest");
            enter("Spawn");
            System.out.println("STRAHD_KREZK_EARLY_VISITS_PASS");
        }); stable();
    }

    private void krezkChapter() {
        action(() -> {
            inspectorBase=owned("Thraben Inspector"); dissenterBase=owned("Doomed Dissenter");
            enter("Saint Andral's Church");
        });
        talk(63); click(1); stable(); // Explicitly decline first.
        action(() -> require(!flag("cosKrezkQuestAccepted"), "Declining Krezk accepted the quest"));
        choose(63,0);
        action(() -> {
            require(flag("cosKrezkQuestAccepted"), "Krezk acceptance missing");
            require(Current.player().getQuests().stream().filter(q -> q.getID()==8103).count()==1, "Krezk quest not issued exactly once");
            enter("Krezk");
        });
        talk(60); click(0); stable(); // Captain must still demand road victory.
        action(() -> {
            require(!flag("cosKrezkAdmitted"), "Captain skipped gate wolves");
            outcome(61,false);
        }); stable();
        action(() -> {
            require(!MapStage.getInstance().isInMap() && !flag("cosKrezkRoadCleared"), "Krezk loss advanced progress or failed to exit");
            enter("Spawn"); enter("Krezk"); BellwakeStorySmoke.victory(61);
        }); reward(); stable();
        action(() -> require(flag("cosKrezkRoadCleared") && !MapStage.getInstance().lookForID(61), "Gate wolves not cleared"));
        choose(60,0);
        action(() -> enter("Krezk Village")); choose(60,0);
        action(() -> {
            require(flag("cosKrezkAdmitted") && flag("cosKrezkHeardDmitri"), "Krezk admission/investigation missing");
            Current.player().heal(-5); gold=Current.player().getGold();
        }); choose(61,0);
        action(() -> {
            require(Current.player().getLife()==Current.player().getMaxLife() && Current.player().getGold()==gold, "Anna recovery failed or charged gold");
            screenshot("krezk-village"); enter("Blessed Pool of Krezk");
        });
        talk(61); click(0); stable();
        action(() -> {
            require(!flag("cosKrezkWaterDrawn"), "Water collected before watcher victory");
            BellwakeStorySmoke.victory(60);
        }); reward(); stable();
        action(() -> {
            require(flag("cosKrezkSpringCleared") && !flag("cosKrezkWaterDrawn"), "Watcher victory collected water");
            require(WorldSave.saveManual("Before Krezk Water"),WorldSave.getLastError()); krezkWater=manual("Before Krezk Water");
        });
        talk(61); click(1); stable();
        action(() -> require(!flag("cosKrezkWaterDrawn"), "Leaving water dialogue collected it"));
        choose(61,0);
        action(() -> {
            require(flag("cosKrezkWaterDrawn"), "Explicit water collection failed");
            require(WorldSave.loadSnapshot(krezkWater),WorldSave.getLastError()); WorldSave.showRestoredExploration();
        }); stable();
        action(() -> require(!flag("cosKrezkWaterDrawn") && !MapStage.getInstance().lookForID(60), "Water rewind lost evidence/cleanup"));
        choose(61,0);
        action(() -> { screenshot("krezk-pool"); enter("Abbey of Saint Markovia"); });
        talk(61); click(0); stable();
        action(() -> require(!flag("cosKrezkSentinelCleared"), "Sentinel skipped Abbot interview"));
        choose(60,0);
        action(() -> BellwakeStorySmoke.victory(61)); reward(); stable();
        action(() -> {
            require(flag("cosKrezkAbbotHeard") && flag("cosKrezkSentinelCleared") && !flag("cosKrezkAccord"), "Abbey victory chose accord");
            require(WorldSave.saveManual("Before Krezk Accord"),WorldSave.getLastError()); krezkAccord=manual("Before Krezk Accord");
        });
        talk(62); click(2); stable();
        action(() -> require(!flag("cosKrezkAccord"), "Leaving accord dialogue made the choice"));
        choose(62,0);
        action(() -> {
            require(Current.player().getCharacterFlag("cosKrezkChoice")==1 && !flag("cosChapterThreeComplete"), "Community accord implicitly completed chapter");
            enter("Krezk Village");
        });
        talk(60); click(2); stable();
        action(() -> require(!flag("cosChapterThreeComplete") && owned("Thraben Inspector")==inspectorBase, "Reward granted on withdrawal"));
        talk(60); click(0); reward(); click(0); stable();
        action(() -> {
            require(flag("cosChapterThreeComplete") && owned("Thraben Inspector")==inspectorBase+2, "White Krezk reward missing");
            require(Current.player().getSelectedDeck().getMain().toCardList("\n").equals(originalDeck), "Krezk edited selected deck");
        });
        talk(60); click(0); stable();
        action(() -> {
            require(owned("Thraben Inspector")==inspectorBase+2, "Krezk reward repeated");
            System.out.println("STRAHD_KREZK_BRANCH_PASS choice=1 reward=white");
            require(WorldSave.loadSnapshot(krezkAccord),WorldSave.getLastError()); WorldSave.showRestoredExploration();
        }); stable();
        action(() -> {
            require(!flag("cosKrezkAccord") && !flag("cosChapterThreeComplete") && owned("Thraben Inspector")==inspectorBase,
                    "Accord rewind retained later rewards/state");
            require(!MapStage.getInstance().lookForID(61), "Abbey sentinel respawned after rewind");
        });
        choose(62,1);
        action(() -> { screenshot("krezk-abbey"); enter("Krezk Village"); });
        talk(60); click(1); reward(); click(0); stable();
        talk(60); click(0); stable();
        action(() -> {
            require(flag("cosChapterThreeComplete") && Current.player().getCharacterFlag("cosKrezkChoice")==2,
                    "Supervised ward accord missing");
            require(owned("Doomed Dissenter")==dissenterBase+2 && owned("Thraben Inspector")==inspectorBase,
                    "Black Krezk reward repeated or rewind leaked white cards");
            require(Current.player().getSelectedDeck().getMain().toCardList("\n").equals(originalDeck), "Black Krezk reward edited deck");
            enter("Krezk"); require(!MapStage.getInstance().lookForID(61), "Gate wolves respawned");
            enter("Blessed Pool of Krezk"); require(!MapStage.getInstance().lookForID(60), "Spring watcher respawned");
            enter("Abbey of Saint Markovia"); require(!MapStage.getInstance().lookForID(61), "Abbey sentinel respawned");
            System.out.println("STRAHD_KREZK_BRANCH_PASS choice=2 reward=black");
        });
        for (String name : new String[]{"Krezk","Krezk Village","Blessed Pool of Krezk","Abbey of Saint Markovia"}) {
            action(() -> prepareLandmarkVisit(name)); stable();
            action(() -> ((forge.adventure.character.EntryActor)MapStage.getInstance().getByID(38)).onPlayerCollide()); stable();
            action(() -> {
                var player=forge.adventure.stage.WorldStage.getInstance().getPlayerSprite();
                require(!MapStage.getInstance().isInMap() && player.getX()==landmarkReturnX && player.getY()==landmarkReturnY,
                        "Krezk southern exit lost overworld approach: "+name);
                System.out.println("STRAHD_KREZK_EXIT_PASS "+name);
            });
        }
        action(() -> System.out.println("STRAHD_KREZK_CHAPTER_PASS"));
    }

    private void choose(int actor, int option) { talk(actor); click(option); click(0); stable(); }

    private void tourLandmarks() {
        var sites = new com.badlogic.gdx.utils.JsonReader().parse(Config.instance().getFile("story/landmarks.json"));
        action(() -> {
            Current.player().heal(-3); tourLife = Current.player().getLife(); tourGold = Current.player().getGold();
            tourCards = Current.player().getCards().toFlatList().size();
        });
        for (var site : sites) {
            if (site.getBoolean("questArea", false)) continue;
            String name = site.getString("name"), slug = site.getString("slug");
            action(() -> prepareLandmarkVisit(name)); stable();
            talk(60); click(0); click(0); stable(); talk(61); click(0); stable();
            // Also exercise the journal-guidance option and immediate dismissal.
            talk(60); click(1); click(0); stable(); talk(60); click(2); stable();
            action(() -> {
                require(Current.player().getLife() == tourLife && Current.player().getGold() == tourGold,
                        "Arrival changed life/currency: " + name);
                require(Current.player().getCards().toFlatList().size() == tourCards &&
                        Current.player().getSelectedDeck().getMain().toCardList("\n").equals(originalDeck),
                        "Arrival changed collection/deck: " + name);
                require(!flag("cosBriefed") && !flag("cosChapterOneComplete") && !flag("cosChapterTwoComplete") &&
                        Current.player().getQuests().size() == 1, "Arrival advanced or added a quest: " + name);
            });
            if (name.equals("Castle Ravenloft") || name.equals("Krezk")) {
                action(() -> {
                    screenshot(slug+"-arrival");
                    require(WorldSave.saveManual("Arrival " + name), WorldSave.getLastError());
                    require(WorldSave.loadSnapshot(manual("Arrival " + name)), WorldSave.getLastError());
                    WorldSave.showRestoredExploration();
                }); stable();
                action(() -> require(TileMapScene.instance().rootPoint.getData().name.equals(name), "Arrival checkpoint restored the wrong scene"));
            }
            action(() -> ((forge.adventure.character.EntryActor) MapStage.getInstance().getByID(38)).onPlayerCollide()); stable();
            action(() -> {
                require(!MapStage.getInstance().isInMap() && Forge.getCurrentScene() instanceof GameScene, "Arrival exit failed: " + name);
                var player = forge.adventure.stage.WorldStage.getInstance().getPlayerSprite();
                require(player.getX() == landmarkReturnX && player.getY() == landmarkReturnY, "Arrival exit changed the overworld return point: " + name);
                System.out.println("STRAHD_LANDMARK_VISIT_PASS " + name);
            });
        }
        action(() -> { enter("Spawn"); System.out.println("STRAHD_LANDMARK_TOUR_PASS total=25"); }); stable();
    }

    private void prepareLandmarkVisit(String name) {
        var world = Current.world();
        var point = world.getAllPointOfInterest().stream().filter(p -> p.getData().name.equals(name)).findFirst().orElseThrow();
        var stage = forge.adventure.stage.WorldStage.getInstance(); var player = stage.getPlayerSprite();
        int tile = world.getTileSize(), cx = (int)point.getPosition().x/tile, cy = (int)point.getPosition().y/tile;
        float best = Float.MAX_VALUE; com.badlogic.gdx.math.Vector2 approach = null;
        for (int x = cx-9; x <= cx+9; x++) for (int y = cy-9; y <= cy+9; y++) {
            if (x < 1 || y < 1 || x >= world.getWidthInTiles()-1 || y >= world.getHeightInTiles()-1) continue;
            var rect = new com.badlogic.gdx.math.Rectangle(x*tile,y*tile,player.getWidth(),player.getHeight());
            if (world.collidingTile(rect)) continue;
            boolean clear = true;
            for (int dx=-1;dx<=1;dx++) for (int dy=-1;dy<=1;dy++) clear &= !world.isColliding(x+dx,y+dy);
            if (!clear || world.getAllPointOfInterest().stream().anyMatch(p -> p.getBoundingRectangle().overlaps(rect))) continue;
            float distance = point.getPosition().dst2(rect.x,rect.y);
            if (distance < best) { best=distance; approach=new com.badlogic.gdx.math.Vector2(rect.x,rect.y); }
        }
        require(approach != null, "No player-sized approach outside marker: " + name);
        landmarkReturnX=approach.x; landmarkReturnY=approach.y;
        player.setPosition(approach.x,approach.y); player.storePos();
        stage.loadPOI(point);
    }

    private static String verifyGeography(String label) {
        var world = Current.world();
        var map = new com.badlogic.gdx.utils.Json().fromJson(forge.adventure.data.AuthoredWorldMapData.class,
                Config.instance().getFile(world.getData().authoredMap));
        map.validate(world.getWidthInTiles(), world.getHeightInTiles(), world.getData().GetBiomes().size());
        long[][] expected = new long[map.width][map.height]; int[][] terrain = new int[map.width][map.height];
        map.paint(expected, terrain); map.paintRoads(expected, terrain, world.getData().GetBiomes().size());
        long hash = 17;
        for (int x = 0; x < map.width; x++) for (int y = 0; y < map.height; y++) {
            int worldY = map.height - y - 1;
            require(world.getBiome(x, worldY) == expected[x][y], "Engine biome differs from authored map at " + x + "," + y);
            require(world.isColliding(x, worldY) == ((terrain[x][y] & Integer.MIN_VALUE) != 0),
                    "Engine collision differs from authored map at " + x + "," + y);
            require(world.terrainMap[x][y] == terrain[x][y], "Engine terrain bits differ at " + x + "," + y);
            hash = hash * 31 + world.getBiome(x, worldY); hash = hash * 31 + world.terrainMap[x][y];
        }
        var start = map.location("Spawn");
        var queue = new java.util.ArrayDeque<Integer>(); var visited = new java.util.HashSet<Integer>();
        queue.add(start.x + start.y * map.width); visited.add(queue.peek());
        while (!queue.isEmpty()) {
            int cell = queue.remove(), x = cell % map.width, y = cell / map.width;
            for (int[] step : new int[][]{{-1,0},{1,0},{0,-1},{0,1}}) {
                int px = x + step[0], py = y + step[1];
                if (px >= 0 && py >= 0 && px < map.width && py < map.height &&
                        !world.isColliding(px, map.height - py - 1) && visited.add(px + py * map.width)) queue.add(px + py * map.width);
            }
        }
        var expectedNames = new java.util.HashSet<String>();
        for (var anchor : map.locations) expectedNames.add(anchor.name);
        var actualNames = new java.util.HashSet<String>();
        for (var point : world.getAllPointOfInterest()) actualNames.add(point.getData().name);
        require(actualNames.equals(expectedNames) && world.getAllPointOfInterest().size() == map.locations.length,
                "Authored POI roster differs after " + label);
        for (var point : world.getAllPointOfInterest()) {
            var anchor = map.location(point.getData().name);
            require(point.getPosition().x == anchor.x * world.getTileSize() &&
                    point.getPosition().y == (map.height - anchor.y - 1) * world.getTileSize(), "POI moved: " + anchor.name);
            require(visited.contains(anchor.x + anchor.y * map.width), "Unreachable engine POI: " + anchor.name);
        }
        Path output = Path.of(System.getProperty("bellwake.screenshot")).getParent().resolve("barovia-" + label + ".png");
        PixmapIO.writePNG(Gdx.files.absolute(output.toString()), world.getBiomeImage(), 6, false);
        System.out.println("STRAHD_AUTHORED_GEOGRAPHY_PASS " + label + " hash=" + Long.toUnsignedString(hash));
        return Long.toUnsignedString(hash);
    }

    private void vallaki(int truth) {
        action(() -> {
            require(Current.player().getQuests().stream().filter(q -> q.getID() == 8102).count() == 1,
                    "Eva did not issue exactly one Vallaki quest");
            require(Current.player().getQuests().stream().filter(q -> q.getID() == 8102).findFirst().orElseThrow().isTracked,
                    "The new journal chapter did not become tracked");
            enter("West Svalich Road");
        });
        talk(60); click(1); stable(); // Leaving a ready encounter must not advance it.
        action(() -> { require(!flag("cosWestRoadCleared"), "Withdraw cleared western road"); outcome(60, false); }); stable();
        action(() -> {
            require(!MapStage.getInstance().isInMap() && !flag("cosWestRoadCleared"), "Western loss advanced or stranded player");
            enter("Vallaki");
        }); talk(61); click(0); click(0); stable();
        action(() -> { require(Current.player().getLife() == Current.player().getMaxLife(), "Urwin did not heal for free"); enter("West Svalich Road"); });
        action(() -> BellwakeStorySmoke.victory(60)); reward(); stable();
        action(() -> {
            require(flag("cosWestRoadCleared") && !MapStage.getInstance().lookForID(60), "Western ambush did not clean up");
            screenshot("west-road"); enter("Vallaki");
        });
        talk(60); click(0); click(0); stable();
        action(() -> { require(flag("cosVallakiEntered"), "Gatekeeper did not admit Ireena"); screenshot("vallaki"); enter("Saint Andral's Church"); });
        talk(60); click(0); click(0); stable();
        action(() -> require(flag("cosBonesInvestigation"), "Lucian did not start investigation"));
        talk(61); click(0); click(0); stable();
        action(() -> { require(flag("cosCoffinLead"), "Milivoj did not reveal lead"); enter("Coffin Maker's Shop"); });
        talk(61); click(0); stable();
        action(() -> require(!flag("cosCoffinCleared"), "Spawn bypassed Henrik's confession"));
        talk(60); click(0); click(0); stable();
        action(() -> {
            require(flag("cosHenrikConfessed"), "Henrik did not confess");
            require(WorldSave.saveManual("Henrik confession " + truth), WorldSave.getLastError());
            require(WorldSave.loadSnapshot(manual("Henrik confession " + truth)), WorldSave.getLastError());
            WorldSave.showRestoredExploration();
        }); stable();
        talk(61);
        action(() -> require(MapStage.getInstance().getDialog().getButtonTable().getChildren().size == 2,
                "Confession reload did not unlock the spawn fight"));
        click(1); stable();
        action(() -> { screenshot("coffin-shop"); outcome(61, false); }); stable();
        action(() -> {
            require(!flag("cosCoffinCleared") && !MapStage.getInstance().isInMap(), "Spawn loss advanced or stranded player");
            enter("Coffin Maker's Shop"); BellwakeStorySmoke.victory(61);
        }); reward(); stable();
        action(() -> {
            require(flag("cosCoffinCleared") && !flag("cosBonesRecovered"), "Victory implicitly collected bones");
            require(!MapStage.getInstance().lookForID(61), "Coffin spawn remained after victory");
            require(WorldSave.saveManual("Before collecting bones " + truth), WorldSave.getLastError());
            relicCheckpoint = manual("Before collecting bones " + truth);
            require(WorldSave.loadSnapshot(relicCheckpoint), WorldSave.getLastError());
            WorldSave.showRestoredExploration();
        }); stable();
        action(() -> require(!MapStage.getInstance().lookForID(61), "Saved spawn reappeared"));
        talk(62); click(1); stable();
        action(() -> require(!flag("cosBonesRecovered"), "Leaving relic dialogue collected bones"));
        talk(62); click(0); click(0); stable();
        action(() -> { require(flag("cosBonesRecovered"), "Relic collection missing"); enter("Saint Andral's Church"); });
        talk(60); click(0); stable();
        action(() -> { require(!flag("cosBonesRestored"), "Restored bones before defending sanctuary"); outcome(62, false); }); stable();
        action(() -> {
            require(!flag("cosSanctuaryDefended") && flag("cosBonesRecovered"), "Belfry loss changed relic state");
            enter("Saint Andral's Church"); BellwakeStorySmoke.victory(62);
        }); reward(); stable();
        action(() -> {
            require(flag("cosSanctuaryDefended") && !MapStage.getInstance().lookForID(62), "Belfry encounter did not clean up");
            require(WorldSave.saveManual("Before truth choice " + truth), WorldSave.getLastError());
            truthCheckpoint = manual("Before truth choice " + truth);
        });
        talk(60); click(truth - 1); click(0); stable();
        action(() -> {
            require(flag("cosBonesRestored") && Current.player().getCharacterFlag("cosVallakiTruth") == truth,
                    "Truth choice did not persist with restoration");
            require(!flag("cosChapterTwoComplete"), "Restoration implicitly completed Ireena's choice");
            require(WorldSave.loadSnapshot(truthCheckpoint), WorldSave.getLastError()); WorldSave.showRestoredExploration();
        }); stable();
        action(() -> require(!flag("cosBonesRestored") && Current.player().getCharacterFlag("cosVallakiTruth") == 0,
                "Earlier checkpoint retained future truth choice"));
        talk(60); click(truth - 1); click(0); stable();
        talk(60); click(0); stable();
        talk(63); click(2); stable();
        action(() -> require(!flag("cosChapterTwoComplete") && owned(chapterUpgrade()) == 0, "Deferred sanctuary granted reward"));
        talk(63); click(starter.equals("B") ? 1 : 0); reward(); click(0); stable();
        action(() -> {
            require(flag("cosChapterTwoComplete") && owned(chapterUpgrade()) == 2, "Vallaki reward/completion missing");
            require(Current.player().getSelectedDeck().getMain().countAll() == 40, "Vallaki reward edited selected deck");
            require(Current.player().getSelectedDeck().getMain().toCardList("\n").equals(originalDeck),
                    "Vallaki reward replaced cards in the selected deck");
            screenshot("saint-andral");
        });
        talk(63); click(1); stable();
        action(() -> {
            require(owned(chapterUpgrade()) == 2, "Sanctuary reward repeated");
            require(!flag("cosKrezkQuestAccepted"), "Sanctuary revisit accepted the Krezk handoff");
            enter("West Svalich Road"); require(!MapStage.getInstance().lookForID(60), "Ambush reappeared on revisit");
            enter("Coffin Maker's Shop"); require(!MapStage.getInstance().lookForID(61), "Spawn reappeared on revisit");
            enter("Saint Andral's Church"); require(!MapStage.getInstance().lookForID(62), "Stalker reappeared on revisit");
            System.out.println("STRAHD_VALLAKI_BRANCH_PASS truth=" + truth + " starter=" + starter);
        });
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
