package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.github.tommyettinger.textra.TextraButton;
import forge.Forge;
import forge.adventure.campaign.CampaignDefinition;
import forge.adventure.campaign.CampaignManager;
import forge.adventure.campaign.CampaignRegistry;
import forge.adventure.data.DifficultyData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.stage.WorldStage;
import forge.adventure.util.AdventureQuestController;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;
import forge.adventure.world.LegacySave;
import forge.adventure.world.PlaythroughMetadata;
import forge.adventure.world.SaveKind;
import forge.adventure.world.SaveSnapshot;
import forge.adventure.world.WorldSave;
import forge.screens.TransitionScreen;
import forge.sound.SoundSystem;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/** Campaign-aware playthrough browser. Each row addresses an identity, never a global slot. */
public class SaveLoadScene extends UIScene {
    private static SaveLoadScene object;
    private final Table rows = new Table();
    private final ScrollPane scrollPane;
    private final Label heading = Controls.newLabel("");
    private final Label details = Controls.newLabel("");
    private final TextraButton action;
    private final TextraButton secondary;
    private final TextraButton backButton;
    private final float rowWidth;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    private Modes mode = Modes.Load;
    private PlaythroughMetadata openedRun;
    private PlaythroughMetadata selectedRun;
    private SaveSnapshot selectedSnapshot;
    private LegacySave selectedLegacy;
    private CampaignRegistry campaigns;
    private boolean legacyView;
    private boolean busy;
    private boolean newManual;
    private final char ASCII_179 = '│';

    private SaveLoadScene() {
        super(Forge.isLandscapeMode() ? "ui/campaign_saves.json" : "ui/campaign_saves_portrait.json");
        Window window = ui.findActor("content");
        window.pad(12);
        rowWidth = window.getWidth() - 36;
        Table body = new Table();
        body.top().left();
        heading.setWrap(true);
        details.setWrap(true);
        body.add(heading).width(rowWidth).left().padBottom(4).row();
        body.add(details).width(rowWidth).minHeight(34).left().padBottom(6).row();
        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle(
                Controls.getSkin().get(ScrollPane.ScrollPaneStyle.class));
        scrollStyle.background = null;
        scrollPane = new ScrollPane(rows, scrollStyle);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        body.add(scrollPane).width(rowWidth).grow();
        window.add(body).grow();
        action = ui.findActor("action");
        secondary = ui.findActor("secondary");
        backButton = ui.findActor("back");
        ui.onButtonPress("action", this::loadSave);
        ui.onButtonPress("secondary", this::secondaryAction);
        ui.onButtonPress("back", this::back);
    }

    public static SaveLoadScene instance() {
        if (object == null)
            object = new SaveLoadScene();
        return object;
    }

    public static void resetForCampaign() {
        Forge.retireAdventureResource(object);
        object = null;
    }

    public enum Modes { Save, Load, NewGamePlus }

    public void setMode(Modes mode) {
        this.mode = mode;
        openedRun = null;
        selectedRun = null;
        selectedSnapshot = null;
        selectedLegacy = null;
        legacyView = false;
        busy = false;
    }

    @Override
    public void enter() {
        campaigns = CampaignRegistry.discover();
        if (mode == Modes.Save)
            openedRun = WorldSave.getCurrentPlaythrough();
        refresh();
        super.enter();
        stage.setScrollFocus(scrollPane);
    }

    private void refresh() {
        unselectActors();
        clearSelectable();
        rows.clear();
        selectedRun = null;
        selectedSnapshot = null;
        selectedLegacy = null;
        newManual = false;
        action.setDisabled(true);
        details.setText("");
        if (mode == Modes.Save && openedRun == null) {
            heading.setText(text("lblSaveGame", "Save game"));
            details.setText(text("lblNoActivePlaythrough", "There is no active playthrough to save."));
        } else if (legacyView) {
            showLegacy();
        } else if (openedRun == null) {
            showRuns();
        } else {
            showSnapshots();
        }
        secondary.setVisible(mode != Modes.Save);
        secondary.setText(legacyView ? text("lblPlaythroughs", "Playthroughs")
                : text("lblImportOldSaves", "Import old saves"));
        if (secondary.isVisible()) addToSelectable(secondary);
        addToSelectable(backButton);
        addToSelectable(action);
        scrollPane.setScrollY(0);
        scrollPane.layout();
        if (!getPossibleSelection().isEmpty())
            selectActor(getPossibleSelection().first());
    }

    private void showRuns() {
        heading.setText(mode == Modes.NewGamePlus ? text("lblNewGame", "New Game") + "+"
                : text("lblLoadPlaythrough", "Load a playthrough"));
        action.setText(text("lblOpen", "Open"));
        List<PlaythroughMetadata> runs = new ArrayList<>(WorldSave.listPlaythroughs());
        Map<String, Long> latestSaves = new HashMap<>();
        for (PlaythroughMetadata run : runs) {
            latestSaves.put(run.playthroughId, latestSave(run));
        }
        runs.sort(Comparator.comparingLong((PlaythroughMetadata run) -> latestSaves.get(run.playthroughId)).reversed());
        if (runs.isEmpty()) {
            details.setText(text("lblNoPlaythroughs", "No playthroughs yet. Start a new game or import an old save."));
        }
        for (PlaythroughMetadata run : runs) {
            String label = run.runName + " — " + run.campaignTitle + "\n"
                    + run.characterName + " · " + dateFormat.format(new Date(latestSaves.get(run.playthroughId)));
            addRow(label, () -> {
                selectedRun = run;
                details.setText(run.campaignTitle + "\n" + run.runName + " — " + run.characterName);
                action.setDisabled(false);
            }, () -> {
                openedRun = run;
                refresh();
            });
        }
    }

    private long latestSave(PlaythroughMetadata run) {
        List<SaveSnapshot> snapshots = WorldSave.listSnapshots(run.playthroughId);
        return snapshots.isEmpty() ? run.createdAt : snapshots.get(0).savedAt;
    }

    private void showSnapshots() {
        heading.setText(openedRun.runName + " — " + openedRun.campaignTitle);
        details.setText(openedRun.characterName);
        action.setText(mode == Modes.Save ? text("lblSave", "Save")
                : mode == Modes.NewGamePlus ? text("lblStart", "Start") : text("lblLoad", "Load"));
        if (mode == Modes.Save) {
            addRow(text("lblNewManualSave", "+ New manual save"), () -> {
                newManual = true;
                selectedSnapshot = null;
                details.setText(text("lblNewCheckpoint", "Create another checkpoint in this playthrough."));
                action.setText(text("lblSave", "Save"));
                action.setDisabled(false);
            }, this::loadSave);
        }
        List<SaveSnapshot> snapshots = WorldSave.listSnapshots(openedRun.playthroughId);
        if (snapshots.isEmpty() && mode != Modes.Save)
            details.setText(text("lblNoCheckpoints", "This playthrough has no saved checkpoints."));
        for (SaveSnapshot snapshot : snapshots) {
            String label = snapshot.label + "\n" + kindLabel(snapshot.kind) + " · "
                    + dateFormat.format(new Date(snapshot.savedAt));
            addRow(label, () -> {
                newManual = false;
                selectedSnapshot = snapshot;
                CampaignDefinition campaign = campaigns.find(snapshot.campaignId, snapshot.campaignVersion);
                String message = snapshot.runName + " — " + snapshot.characterName;
                if (snapshot.location != null && !snapshot.location.isEmpty())
                    message += "\n" + snapshot.location;
                if (campaign == null)
                    message += "\n" + text("lblMissingCampaign", "Required campaign is unavailable:")
                            + " " + snapshot.campaignTitle + " " + snapshot.campaignVersion;
                details.setText(message);
                if (mode == Modes.Save) {
                    action.setText(text("lblReplaceSave", "Replace save"));
                    action.setDisabled(snapshot.kind != SaveKind.MANUAL);
                } else {
                    action.setDisabled(campaign == null || (mode == Modes.NewGamePlus && !campaign.isNewGamePlusAllowed()));
                    if (campaign != null && mode == Modes.NewGamePlus && !campaign.isNewGamePlusAllowed())
                        details.setText(text("lblCampaignNoNewGamePlus", "This campaign does not offer New Game+."));
                }
            }, this::loadSave);
        }
    }

    private void showLegacy() {
        heading.setText(text("lblImportOldSaves", "Import old saves"));
        details.setText(text("lblImportSaveHelp", "Import a copy as a separate playthrough. Original saves stay in place."));
        action.setText(text("lblImport", "Import"));
        List<LegacySave> saves = WorldSave.listLegacySaves();
        if (saves.isEmpty())
            details.setText(text("lblNoOldSaves", "No old save files were found."));
        for (LegacySave save : saves) {
            addRow(save.campaignTitle + " — " + save.fileName + "\n"
                    + dateFormat.format(new Date(save.modifiedAt)), () -> {
                selectedLegacy = save;
                details.setText(save.campaignTitle + "\n" + save.fileName);
                action.setDisabled(false);
            }, this::loadSave);
        }
    }

    private void addRow(String label, Runnable selected, Runnable activate) {
        TextraButton button = Controls.newTextButton(label);
        button.getTextraLabel().setWrap(true);
        Selectable<TextraButton> selectable = new Selectable<TextraButton>(button) {
            @Override
            public void onSelect(UIScene scene) { selected.run(); }
        };
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectActor(selectable);
                if (getTapCount() > 1 && !action.isDisabled()) activate.run();
            }
        });
        rows.add(button).width(rowWidth - 6).minHeight(34).growX().padBottom(3).row();
        addToSelectable(selectable);
    }

    private void secondaryAction() {
        if (busy) return;
        legacyView = !legacyView;
        openedRun = null;
        refresh();
    }

    public void loadSave() {
        if (busy || action.isDisabled()) return;
        if (legacyView && selectedLegacy != null) {
            promptImport(selectedLegacy);
        } else if (openedRun == null && selectedRun != null) {
            openedRun = selectedRun;
            refresh();
        } else if (mode == Modes.Save && (newManual || selectedSnapshot != null)) {
            promptSave(newManual ? null : selectedSnapshot);
        } else if (selectedSnapshot != null) {
            if (mode == Modes.NewGamePlus) promptNewGamePlus(selectedSnapshot);
            else load(selectedSnapshot);
        }
    }

    private void promptSave(SaveSnapshot replace) {
        TextField name = Controls.newTextField(replace == null
                ? text("lblCheckpoint", "Checkpoint") + " " + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date())
                : replace.label);
        name.setMaxLength(100);
        String help = replace == null ? text("lblNameYourSaveFile", "Name your save")
                : text("lblReplaceCheckpointPrompt", "Replace this manual checkpoint? Other saves will be kept.");
        Dialog prompt = createGenericDialog(text("lblSaveGame", "Save game"), null,
                text("lblSave", "Save"), text("lblCancel", "Cancel"), () -> {
                    String label = name.getText().trim();
                    if (label.isEmpty()) label = text("lblCheckpoint", "Checkpoint");
                    boolean saved = replace == null ? WorldSave.saveManual(label) : WorldSave.replaceManual(replace, label);
                    removeDialog();
                    if (saved) Forge.switchScene(GameScene.instance());
                    else error(WorldSave.getLastError());
                }, this::removeDialog);
        addPromptField(prompt, help, name);
        showDialog(prompt);
        stage.setKeyboardFocus(name);
    }

    private void promptImport(LegacySave legacy) {
        TextField name = Controls.newTextField(text("lblImportedSave", "Imported") + " " + legacy.fileName);
        name.setMaxLength(80);
        Dialog prompt = createGenericDialog(text("lblImportOldSaves", "Import old saves"), null,
                text("lblImport", "Import"), text("lblCancel", "Cancel"), () -> {
                    SaveSnapshot imported = WorldSave.importLegacy(legacy, name.getText().trim());
                    removeDialog();
                    if (imported == null) error(WorldSave.getLastError());
                    else {
                        legacyView = false;
                        openedRun = null;
                        for (PlaythroughMetadata run : WorldSave.listPlaythroughs()) {
                            if (run.playthroughId.equals(imported.playthroughId)) {
                                openedRun = run;
                                break;
                            }
                        }
                        refresh();
                    }
                }, this::removeDialog);
        addPromptField(prompt, text("lblPlaythroughName", "Playthrough name"), name);
        showDialog(prompt);
        stage.setKeyboardFocus(name);
    }

    private void load(SaveSnapshot snapshot) {
        busy = true;
        Forge.setTransitionScreen(new TransitionScreen(() -> {
            try {
                if (WorldSave.loadSnapshot(snapshot)) {
                    SoundSystem.instance.changeBackgroundTrack();
                    Forge.switchScene(GameScene.instance());
                } else loadFailed(WorldSave.getLastError());
            } catch (RuntimeException e) {
                loadFailed(e.getMessage());
            } finally {
                busy = false;
            }
        }, null, false, true, text("lblLoadingWorld", "Loading world")));
    }

    private void promptNewGamePlus(SaveSnapshot snapshot) {
        CampaignDefinition campaign = campaigns.find(snapshot.campaignId, snapshot.campaignVersion);
        if (campaign == null || !campaign.isNewGamePlusAllowed()) return;
        DifficultyData[] options = Config.forCampaign(campaign).getConfigData().difficulties;
        SelectBox<String> difficulty = Controls.newComboBox();
        com.badlogic.gdx.utils.Array<String> names = new com.badlogic.gdx.utils.Array<>();
        for (DifficultyData option : options) names.add(text("lbl" + option.name, option.name));
        difficulty.setItems(names);
        if (options.length > 1) difficulty.setSelectedIndex(1);
        TextField name = Controls.newTextField(snapshot.runName + " +");
        name.setMaxLength(80);
        Dialog prompt = createGenericDialog(text("lblNewGame", "New Game") + "+", null,
                text("lblStart", "Start"), text("lblCancel", "Cancel"), () -> {
                    String runName = name.getText().trim();
                    int selectedDifficulty = difficulty.getSelectedIndex();
                    removeDialog();
                    newGamePlus(snapshot, runName.isEmpty() ? snapshot.runName + " +" : runName, selectedDifficulty);
                }, this::removeDialog);
        addPromptField(prompt, text("lblNewGamePlusPreservesRun", "Start a new run. The source playthrough and its saves will be kept."), name);
        prompt.getContentTable().row();
        prompt.getContentTable().add(difficulty).width(220).pad(5);
        showDialog(prompt);
        stage.setKeyboardFocus(name);
    }

    private void newGamePlus(SaveSnapshot source, String runName, int difficultyIndex) {
        busy = true;
        Forge.setTransitionScreen(new TransitionScreen(() -> {
            boolean newRun = false;
            try {
                if (!WorldSave.loadSnapshot(source)) {
                    loadFailed(WorldSave.getLastError());
                    return;
                }
                WorldSave.beginNewGamePlus(runName);
                newRun = true;
                WorldSave.getCurrentSave().clearChanges();
                if (!WorldSave.getCurrentSave().getWorld().generateNew(0))
                    throw new IllegalStateException(text("lblWorldGenerationFailed", "The new world could not be generated."));
                DifficultyData[] options = Config.instance().getConfigData().difficulties;
                if (difficultyIndex >= 0 && difficultyIndex < options.length)
                    Current.player().updateDifficulty(options[difficultyIndex]);
                Current.player().setWorldPosY((int) (Current.world().getData().playerStartPosY
                        * Current.world().getData().height * Current.world().getTileSize()));
                Current.player().setWorldPosX((int) (Current.world().getData().playerStartPosX
                        * Current.world().getData().width * Current.world().getTileSize()));
                Current.player().getQuests().clear();
                AdventureQuestController.clear();
                Current.player().resetQuestFlags();
                Current.player().setCharacterFlag("newGamePlus", 1);
                Current.player().removeAllQuestItems();
                WorldSave.getCurrentSave().clearBookmarks();
                CampaignManager.applyStartingQuests();
                if (!WorldSave.saveInitialSnapshot()) {
                    loadFailed(text("lblInitialSaveFailed", "The new playthrough is active, but its first save failed.")
                            + "\n" + WorldSave.getLastError());
                    return;
                }
                WorldStage.getInstance().enterSpawnPOI();
                SoundSystem.instance.changeBackgroundTrack();
                Forge.switchScene(GameScene.instance());
            } catch (RuntimeException e) {
                String failure = e.getMessage();
                if (newRun) {
                    WorldSave.discardFailedPlaythrough();
                    WorldSave.loadSnapshot(source);
                }
                loadFailed(failure);
            } finally {
                busy = false;
            }
        }, null, false, true, text("lblGeneratingWorld", "Generating world")));
    }

    private void addPromptField(Dialog prompt, String help, TextField input) {
        Label label = Controls.newLabel(help);
        label.setWrap(true);
        prompt.getContentTable().add(label).width(220).pad(5).row();
        prompt.getContentTable().add(input).width(220).pad(5);
    }

    private void loadFailed(String message) {
        Forge.clearTransitionScreen();
        StartScene menu = StartScene.instance();
        Forge.switchScene(menu);
        menu.showError(message == null ? text("lblLoadingFailed", "The saved game could not be loaded.") : message);
    }

    private void error(String message) {
        showDialog(createGenericDialog(text("lblError", "Error"), message, text("lblOK", "OK"),
                null, this::removeDialog, null, false, "", true));
    }

    @Override
    public boolean back() {
        if (busy) return true;
        if (mode != Modes.Save && (openedRun != null || legacyView)) {
            openedRun = null;
            legacyView = false;
            refresh();
        } else Forge.switchScene(StartScene.instance());
        return true;
    }

    public void showMessage(String title, String message) {
        showDialog(createGenericDialog(title, message, text("lblOK", "OK"), null,
                this::removeDialog, null, false, "", true));
    }

    private static String kindLabel(SaveKind kind) {
        switch (kind) {
            case AUTO: return text("lblAutoSave", "Autosave");
            case QUICK: return text("lblQuickSave", "Quicksave");
            case INITIAL: return text("lblInitialSave", "Beginning");
            case IMPORTED: return text("lblImportedSave", "Imported");
            default: return text("lblManualSave", "Manual save");
        }
    }

    private static String text(String key, String fallback) {
        return Forge.getLocalizer().getMessageorUseDefault(key, fallback);
    }

    public String getSaveFileSuffix() {
        String difficulty;
        switch (AdventurePlayer.current().getDifficulty().name) {
            case "easy":
            case "Easy":
                difficulty = "[%99][CYAN]• [WHITE]";
                break;
            case "normal":
            case "Normal":
                difficulty = "[%99][GREEN]• [WHITE]";
                break;
            case "hard":
            case "Hard":
                difficulty = "[%99][GOLD]• [WHITE]";
                break;
            case "insane":
            case "Insane":
                difficulty = "[%99][RED]• [WHITE]";
                break;
            default:
                difficulty = "[%99][WHITE]";
                break;
        }
        return ASCII_179 + difficulty + GameScene.instance().getAdventurePlayerLocation(true, true);
    }
}
