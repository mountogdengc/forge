package forge.app;

import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import forge.Forge;
import forge.adventure.campaign.CampaignManager;
import forge.adventure.scene.GameScene;
import forge.adventure.scene.RewardScene;
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

/** Assisted state/interaction coverage. Supplies victories; never report this as balance or a played match. */
final class BellwakeStorySmoke {
    private int step;
    private int ending = 1;
    private int practiceCards;
    private SaveSnapshot ticket;
    private SaveSnapshot finished;
    private boolean ticketTaken;
    private final String starter = System.getProperty("bellwake.starter", "W");
    private final String practiceUpgrade = starter.equals("R") ? "Lightning Strike" : "Pacifism";

    void begin() {
        var campaign = CampaignManager.current();
        require(WorldSave.checkpointAndSuspend(), WorldSave.getLastError());
        CampaignManager.activate(campaign);
        WorldSave.beginPlaythrough(campaign, "Story interaction smoke", "Story Smoke");
        WorldSave.generateNewWorld("Story Smoke", true, 0, 0, ColorSet.fromNames(starter),
                Config.instance().getConfigData().difficulties[0], AdventureModes.Constructed, 0, null, 0);
        var quest = AdventureQuestController.instance().generateQuest(7001);
        quest.prologue = null; // Opening text is covered by content tests; avoid a timed queued prologue in this fixture.
        Current.player().addQuest(quest, true);
        require(WorldSave.saveInitialSnapshot(), WorldSave.getLastError());
        Forge.switchScene(GameScene.instance());
        enter("Spawn");
        practiceCards = owned(practiceUpgrade);
    }

    boolean tick() throws Exception {
        System.out.println("BELLWAKE_STORY_SMOKE step=" + step + " ending=" + ending + " policy=" + WorldSave.explorationSaveBlockReason());
        switch (step) {
            case 0: ((forge.adventure.character.DialogActor) MapStage.getInstance().getByID(60)).onPlayerCollide(); break;
            case 1: press(0); break;
            case 2:
                press(0);
                require(Current.player().checkQuestFlag("bellwakeBriefed"), "Mira did not start the investigation");
                break;
            case 3:
                if (!WorldSave.explorationSaveBlockReason().isEmpty()) return false;
                victory(61); break;
            case 4:
                if (!(Forge.getCurrentScene() instanceof RewardScene)) return false;
                finishRewards(); break;
            case 5: press(starter.equals("R") ? 1 : 0); break;
            case 6: finishRewards(); break;
            case 7:
                press(0);
                require(owned(practiceUpgrade) == practiceCards + 2, "Practice reward missing");
                require(Current.player().getSelectedDeck().getMain().countAll() == 40, "Reward silently edited the deck");
                break;
            case 8:
                if (!WorldSave.explorationSaveBlockReason().isEmpty()) return false;
                victory(61); break;
            case 9:
                if (!(Forge.getCurrentScene() instanceof RewardScene)) return false;
                finishRewards(); break;
            case 10:
                press(0);
                require(owned(practiceUpgrade) == practiceCards + 2, "Practice reward repeated");
                enter("Bellwake Tollhouse");
                break;
            case 11: victory(60); break;
            case 12:
                if (!(Forge.getCurrentScene() instanceof RewardScene)) return false;
                finishRewards(); break;
            case 13:
                if (!ticketTaken) {
                    press(0);
                    ticketTaken = true;
                    return false;
                }
                if (!WorldSave.explorationSaveBlockReason().isEmpty()) return false;
                require(Current.player().checkQuestFlag("bellwakeFareReleased"), "Ticket action did not advance the quest");
                require(!MapStage.getInstance().lookForID(60), "Ticket action did not remove the Keeper");
                require(WorldSave.saveManual("Story ticket checkpoint"), WorldSave.getLastError());
                ticket = manual("Story ticket checkpoint");
                enter("Spawn");
                break;
            case 14: ((forge.adventure.character.DialogActor) MapStage.getInstance().getByID(60)).onPlayerCollide(); break;
            case 15: press(ending - 1); break;
            case 16: press(0); break;
            case 17:
                require(Forge.getCurrentScene() instanceof RewardScene, "Ending reward screen missing");
                int count = WorldSave.listSnapshots(WorldSave.getCurrentPlaythrough().playthroughId).size();
                require(!WorldSave.saveManual("Invalid partial ending"), "Saved an unfinished reward interaction");
                WorldSave.requestExplorationCheckpoint("Ending interaction probe");
                WorldSave.processExplorationCheckpoint();
                require(WorldSave.listSnapshots(WorldSave.getCurrentPlaythrough().playthroughId).size() == count,
                        "Deferred checkpoint published during reward selection");
                finishRewards();
                break;
            case 18: press(0); break;
            case 19:
                if (!WorldSave.explorationSaveBlockReason().isEmpty()) return false;
                require(Current.player().getCharacterFlag("bellwakeEnding") == ending, "Wrong ending flag");
                require(Current.player().checkQuestFlag("bellwakeResolved"), "Story did not resolve");
                require(owned("Ghoulcaller's Bell") == (ending == 1 ? 1 : 0), "Wrong Bell reward count");
                require(owned("Traveler's Amulet") == (ending == 2 ? 1 : 0), "Wrong Amulet reward count");
                require(WorldSave.saveManual("Story ending " + ending), WorldSave.getLastError());
                finished = manual("Story ending " + ending);
                break;
            case 20:
                require(WorldSave.loadSnapshot(finished), WorldSave.getLastError());
                WorldSave.showRestoredExploration();
                require(Current.player().getCharacterFlag("bellwakeEnding") == ending, "Ending not restored");
                break;
            case 21: ((forge.adventure.character.DialogActor) MapStage.getInstance().getByID(60)).onPlayerCollide(); break;
            case 22: press(1); break;
            case 23:
                if (!WorldSave.explorationSaveBlockReason().isEmpty()) return false;
                require(owned("Ghoulcaller's Bell") == (ending == 1 ? 1 : 0), "Revisit repeated Bell reward");
                require(owned("Traveler's Amulet") == (ending == 2 ? 1 : 0), "Revisit repeated Amulet reward");
                if (ending == 1) {
                    require(WorldSave.loadSnapshot(ticket), WorldSave.getLastError());
                    WorldSave.showRestoredExploration();
                    require(!MapStage.getInstance().lookForID(60), "Pre-choice checkpoint resurrected Keeper");
                    require(owned("Ghoulcaller's Bell") == 0, "Old checkpoint retained future reward");
                    enter("Spawn");
                    ending = 2;
                    step = 14;
                    return false;
                }
                return true;
            default: throw new AssertionError("Invalid story smoke step");
        }
        step++;
        return false;
    }

    private static SaveSnapshot manual(String label) {
        return WorldSave.listSnapshots(WorldSave.getCurrentPlaythrough().playthroughId).stream()
                .filter(s -> s.kind == SaveKind.MANUAL && s.label.equals(label)).findFirst().orElseThrow();
    }

    private static int owned(String name) {
        return (int) Current.player().getCards().toFlatList().stream().filter(c -> c.getName().equals(name)).count();
    }

    private static void enter(String name) {
        var point = Current.world().getAllPointOfInterest().stream().filter(p -> p.getData().name.equals(name)).findFirst().orElseThrow();
        TileMapScene.instance().load(point);
        Forge.switchScene(TileMapScene.instance());
    }

    private static void press(int index) {
        var buttons = MapStage.getInstance().getDialog().getButtonTable().getChildren();
        require(buttons.size > index, "Dialogue choice missing at index " + index);
        var button = buttons.get(index);
        var listeners = new com.badlogic.gdx.utils.Array<>(button.getListeners());
        for (var listener : listeners) if (listener instanceof ClickListener) ((ClickListener) listener).clicked(null, 0, 0);
    }

    static void victory(int id) throws Exception {
        var stage = MapStage.getInstance();
        Current.setLatestDeck(stage.getEnemyByID(id).getData().generateDeck(Current.player().isFantasyMode(), false));
        require(Current.latestDeck() != null && Current.latestDeck().getMain().countAll() == 40, "Encounter deck did not generate");
        var field = MapStage.class.getDeclaredField("currentMob");
        field.setAccessible(true);
        field.set(stage, stage.getEnemyByID(id));
        stage.setWinner(true, false);
        require(!WorldSave.saveManual("Unsafe victory checkpoint"), "A partial victory was saved before its reward callback");
    }

    static void finishRewards() throws Exception {
        require(Forge.getCurrentScene() instanceof RewardScene, "Expected reward collection screen");
        var method = RewardScene.class.getDeclaredMethod("done", boolean.class);
        method.setAccessible(true);
        method.invoke(RewardScene.instance(), true); // Skip flip animation; grant through the production collection path.
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
