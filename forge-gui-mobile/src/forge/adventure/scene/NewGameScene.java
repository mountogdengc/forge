package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;

import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.campaign.CampaignDefinition;
import forge.adventure.campaign.CampaignManager;
import forge.adventure.data.DialogData;
import forge.adventure.data.DifficultyData;
import forge.adventure.data.HeroListData;
import forge.adventure.stage.WorldStage;
import forge.adventure.util.*;
import forge.adventure.world.WorldSave;
import forge.card.CardEdition;
import forge.card.ColorSet;
import forge.deck.DeckProxy;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.screens.TransitionScreen;
import forge.sound.SoundSystem;
import forge.util.Localizer;
import forge.util.NameGenerator;

import java.util.Random;

/**
 * NewGame scene that contains the character creation
 */
public class NewGameScene extends MenuScene {

    private final CampaignDefinition campaign;
    private final Config previewConfig;
    private final HeroListData previewHeroes;
    TextField selectedName;
    ColorSet[] colorIds;
    CardEdition[] editionIds;
    private final Image avatarImage;
    private int avatarIndex = 0;
    private final Selector race;
    private final Selector colorId;
    private final Selector gender;
    private final Selector mode;
    private final Selector difficulty;
    private final Selector starterEdition;
    private final TextraLabel starterEditionLabel;
    private final Array<String> custom;
    private final TextraLabel colorLabel;
    private final ImageButton difficultyHelp;
    private DialogData difficultySummary;
    private final ImageButton modeHelp;
    private DialogData modeSummary;
    private final Random rand = new Random();
    private String originalEditionLabelText;
    private Array<String> originalEditionNames;

    private final Array<AdventureModes> modes = new Array<>();

    private NewGameScene(CampaignDefinition campaign, Config config) {
        super(Forge.isLandscapeMode() ? "ui/new_game.json" : "ui/new_game_portrait.json", config);
        this.campaign = campaign;
        previewConfig = config;
        previewHeroes = HeroListData.forConfig(config);
        gender = ui.findActor("gender");
        selectedName = ui.findActor("nameField");
        generateName();
        avatarImage = ui.findActor("avatarPreview");
        mode = ui.findActor("mode");
        modeHelp = ui.findActor("modeHelp");
        colorLabel = ui.findActor("colorIdL");
        String colorIdLabel = colorLabel.storedText;
        String deckLabel = "[BLACK]" + Forge.getLocalizer().getMessage("lblDeck") + ":";
        custom = new Array<>();
        colorId = ui.findActor("colorId");
        String[] colorSet = previewConfig.colorIds();
        String[] colorIdNames = previewConfig.colorIdNames();
        colorIds = new ColorSet[colorSet.length];
        for (int i = 0; i < colorIds.length; i++)
            colorIds[i] = ColorSet.fromNames(colorSet[i].toCharArray());
        Array<String> colorNames = new Array<>(colorIds.length);
        for (String idName : colorIdNames)
            colorNames.add(UIActor.localize(idName));
        colorId.setTextList(colorNames);

        for (DifficultyData diff : previewConfig.getConfigData().difficulties)//check first difficulty if exists
        {
            if (diff.starterDecks != null) {
                modes.add(AdventureModes.Standard);
                AdventureModes.Standard.setSelectionName(colorIdLabel);
                AdventureModes.Standard.setModes(colorNames);
            }

            if (diff.constructedStarterDecks != null) {
                modes.add(AdventureModes.Constructed);
                AdventureModes.Constructed.setSelectionName(colorIdLabel);
                AdventureModes.Constructed.setModes(colorNames);
            }
            if (diff.pileDecks != null) {
                modes.add(AdventureModes.Pile);
                AdventureModes.Pile.setSelectionName(colorIdLabel);
                AdventureModes.Pile.setModes(colorNames);
            }
            if (diff.commanderDecks != null) {
                modes.add(AdventureModes.Commander);
                AdventureModes.Commander.setSelectionName(colorIdLabel);
                AdventureModes.Commander.setModes(colorNames);
            }
            break;
        }

        starterEdition = ui.findActor("starterEdition");
        starterEditionLabel = ui.findActor("starterEditionL");
        originalEditionLabelText = starterEditionLabel.storedText;
        String[] starterEditions = previewConfig.starterEditions();
        String[] starterEditionNames = previewConfig.starterEditionNames();
        editionIds = new CardEdition[starterEditions.length];
        for (int i = 0; i < editionIds.length; i++)
            editionIds[i] = FModel.getMagicDb().getEditions().get(starterEditions[i]);
        originalEditionNames = new Array<>(editionIds.length);
        for (String editionName : starterEditionNames)
            originalEditionNames.add(UIActor.localize(editionName));
        starterEdition.setTextList(originalEditionNames);

        // Precon mode: deck names in colorId, set filter in starterEdition
        if (previewConfig.hasPreconDecks()) {
            modes.add(AdventureModes.Precon);
            AdventureModes.Precon.setSelectionName(deckLabel);
            AdventureModes.Precon.setModes(previewConfig.filterPreconDecks(0));
        }

        if (previewConfig.hasCommanderPreconDecks()) {
            modes.add(AdventureModes.CommanderPrecon);
            AdventureModes.CommanderPrecon.setSelectionName(deckLabel);
            AdventureModes.CommanderPrecon.setModes(previewConfig.filterCommanderPreconDecks(0));
        }

        modes.add(AdventureModes.Chaos);
        AdventureModes.Chaos.setSelectionName(deckLabel);
        AdventureModes.Chaos.setModes(new Array<>(new String[]{Forge.getLocalizer().getMessage("lblRandomDeck")}));
        for (DeckProxy deckProxy : DeckProxy.getAllCustomStarterDecks())
            custom.add(deckProxy.getName());
        if (!custom.isEmpty()) {
            modes.add(AdventureModes.Custom);
            AdventureModes.Custom.setSelectionName(deckLabel);
            AdventureModes.Custom.setModes(custom);
        }

        String[] modeNames = new String[modes.size];
        int constructedIndex = -1;

        for (int i = 0; i < modes.size; i++) {
            modeNames[i] = modes.get(i).getName();
            if (modes.get(i) == AdventureModes.Constructed) {
                constructedIndex = i;
            }
        }

        mode.setTextList(modeNames);
        mode.setCurrentIndex(constructedIndex != -1 ? constructedIndex : 0);

        AdventureModes initialMode = modes.get(mode.getCurrentIndex());
        updateModeSelectionState(initialMode);

        gender.setTextList(new String[]{Forge.getLocalizer().getMessage("lblMale") + "[%120][CYAN] \u2642",
                Forge.getLocalizer().getMessage("lblFemale") + "[%120][MAGENTA] \u2640"});
        gender.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                nameTT = 0.8f;
                super.clicked(event, x, y);
            }
        });
        gender.addListener(event -> NewGameScene.this.updateAvatar());

        mode.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                updateModeSelectionState(modes.get(mode.getCurrentIndex()));
            }
        });
        starterEdition.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                AdventureModes smode = modes.get(mode.getCurrentIndex());
                if (smode == AdventureModes.Precon) {
                    colorId.setTextList(previewConfig.filterPreconDecks(starterEdition.getCurrentIndex()));
                } else if (smode == AdventureModes.CommanderPrecon) {
                    colorId.setTextList(previewConfig.filterCommanderPreconDecks(starterEdition.getCurrentIndex()));
                }
            }
        });
        race = ui.findActor("race");
        race.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                avatarTT = 0.7f;
                super.clicked(event, x, y);
            }
        });
        race.addListener(event -> NewGameScene.this.updateAvatar());
        race.setTextList(previewHeroes.getRaces());
        difficulty = ui.findActor("difficulty");
        difficultyHelp = ui.findActor("difficultyHelp");

        Array<String> diffList = new Array<>(colorIds.length);
        int i = 0;
        int startingDifficulty = 0;
        for (DifficultyData diff : previewConfig.getConfigData().difficulties) {
            if (diff.startingDifficulty)
                startingDifficulty = i;
            diffList.add(Forge.getLocalizer().getMessageorUseDefault("lbl" + diff.name, diff.name));
            i++;
        }
        difficulty.setTextList(diffList);
        difficulty.setCurrentIndex(startingDifficulty);

        generateAvatar();
        gender.setCurrentIndex(rand.nextInt());
        colorId.setCurrentIndex(rand.nextInt());
        race.setCurrentIndex(rand.nextInt());
        ui.onButtonPress("back", NewGameScene.this::back);
        ui.onButtonPress("start", NewGameScene.this::start);
        ui.onButtonPress("leftAvatar", NewGameScene.this::leftAvatar);
        ui.onButtonPress("rightAvatar", NewGameScene.this::rightAvatar);
        difficultyHelp.addListener(new ClickListener() {
            public void clicked(InputEvent e, float x, float y) {
                showDifficultyHelp();
            }
        });
        modeHelp.addListener(new ClickListener() {
            public void clicked(InputEvent e, float x, float y) {
                showModeHelp();
            }
        });
    }

    // class field
    private RewardActor previewActor;

    private static NewGameScene object;

    public static NewGameScene instance() {
        if (object == null)
            return forCampaign(CampaignManager.current());
        return object;
    }

    public static NewGameScene forCampaign(CampaignDefinition campaign) {
        NewGameScene next = new NewGameScene(campaign, Config.forCampaign(campaign));
        Forge.retireAdventureResource(object);
        object = next;
        return object;
    }

    public static void resetForCampaign() {
        Forge.retireAdventureResource(object);
        object = null;
    }

    float avatarT = 1f, avatarTT = 1f;
    float nameT = 1f, nameTT = 1f;

    @Override
    public void act(float delta) {
        super.act(delta);
        if (avatarT > avatarTT) {
            avatarTT += (delta / 0.5f);
            generateAvatar();
        } else {
            avatarTT = avatarT;
        }
        if (nameT > nameTT) {
            nameTT += (delta / 0.5f);
            generateName();
        } else {
            nameTT = nameT;
        }
    }

    private void generateAvatar() {
        avatarIndex = rand.nextInt();
        updateAvatar();
    }

    private void generateName() {
        //gender should be either Male or Female
        String val = gender.getCurrentIndex() > 0 ? "Female" : "Male";
        selectedName.setText(NameGenerator.getRandomName(val, "Any", ""));
    }

    private void updateModeSelectionState(AdventureModes selectedMode) {
        colorLabel.setText(selectedMode.getSelectionName());
        boolean showEdition = selectedMode.usesStarterEditionSelector();
        starterEdition.setVisible(showEdition);
        starterEditionLabel.setVisible(showEdition);

        if (selectedMode == AdventureModes.Precon) {
            starterEdition.setTextList(previewConfig.getPreconSetNames());
            starterEditionLabel.setText("[BLACK]" + Forge.getLocalizer().getMessageorUseDefault("lblEdition", "Edition") + ":");
            colorId.setTextList(previewConfig.filterPreconDecks(starterEdition.getCurrentIndex()));
        } else if (selectedMode == AdventureModes.CommanderPrecon) {
            starterEdition.setTextList(previewConfig.getCommanderPreconSetNames());
            starterEditionLabel.setText("[BLACK]" + Forge.getLocalizer().getMessageorUseDefault("lblEdition", "Edition") + ":");
            colorId.setTextList(previewConfig.filterCommanderPreconDecks(starterEdition.getCurrentIndex()));
        } else if (selectedMode == AdventureModes.Standard) {
            starterEditionLabel.setText(originalEditionLabelText);
            starterEdition.setTextList(originalEditionNames);
            colorId.setTextList(selectedMode.getModes());
        } else {
            colorId.setTextList(selectedMode.getModes());
        }
    }

    private ColorSet getStartingColor() {
        AdventureModes currentMode = modes.get(mode.getCurrentIndex());
        if (currentMode.usesFolderDeckPicker() || currentMode == AdventureModes.Chaos) {
            return ColorSet.fromNames("W".toCharArray());
        }
        if (currentMode == AdventureModes.Custom) {
            return colorIds[0];
        }
        int idx = colorId.getCurrentIndex();
        return colorIds[idx < colorIds.length ? idx : 0];
    }

    private CardEdition getStartingEdition() {
        AdventureModes currentMode = modes.get(mode.getCurrentIndex());
        if (currentMode == AdventureModes.Standard && editionIds.length > 0) {
            int idx = starterEdition.getCurrentIndex();
            return editionIds[idx < editionIds.length ? idx : 0];
        }
        return editionIds.length > 0 ? editionIds[0] : null;
    }

    boolean started = false;

    public boolean start() {
        if (started)
            return true;
        started = true;
        if (selectedName.getText().trim().isEmpty())
            generateName();

        TextField runName = Controls.newTextField(selectedName.getText());
        runName.setMaxLength(80);
        Dialog prompt = createGenericDialog(
                Forge.getLocalizer().getMessageorUseDefault("lblNewPlaythrough", "New playthrough"),
                null, Forge.getLocalizer().getMessage("lblStart"),
                Forge.getLocalizer().getMessage("lblCancel"), () -> {
                    String name = runName.getText().trim();
                    removeDialog();
                    createPlaythrough(name.isEmpty() ? selectedName.getText() : name);
                }, () -> {
                    started = false;
                    removeDialog();
                });
        prompt.getContentTable().add(Controls.newLabel(campaign.getTitle())).colspan(2).width(220).pad(5);
        prompt.getContentTable().row();
        prompt.getContentTable().add(Controls.newLabel(
                Forge.getLocalizer().getMessageorUseDefault("lblPlaythroughName", "Playthrough name"))).pad(5);
        prompt.getContentTable().row();
        prompt.getContentTable().add(runName).width(220).pad(5);
        showDialog(prompt);
        stage.setKeyboardFocus(runName);
        return true;
    }

    private void createPlaythrough(String runName) {
        // Capture all options before campaign activation invalidates scene caches.
        final String characterName = selectedName.getText().trim();
        final boolean male = gender.getCurrentIndex() == 0;
        final int selectedRace = race.getCurrentIndex();
        final int selectedAvatar = avatarIndex;
        final ColorSet color = getStartingColor();
        final DifficultyData selectedDifficulty = previewConfig.getConfigData().difficulties[difficulty.getCurrentIndex()];
        final AdventureModes selectedMode = modes.get(mode.getCurrentIndex());
        final int deckIndex = colorId.getCurrentIndex();
        final CardEdition edition = getStartingEdition();
        if (!WorldSave.checkpointAndSuspend()) {
            started = false;
            showDialog(createGenericDialog(Forge.getLocalizer().getMessage("lblError"),
                    WorldSave.getLastError(), Forge.getLocalizer().getMessage("lblOK"),
                    null, this::removeDialog, null, false, "", true));
            return;
        }
        Runnable runnable = () -> {
            try {
                CampaignManager.activate(campaign);
                WorldSave.beginPlaythrough(campaign, runName, characterName);
                WorldSave.generateNewWorld(characterName, male, selectedRace, selectedAvatar, color,
                        selectedDifficulty, selectedMode, deckIndex, edition, 0);
                GamePlayerUtil.getGuiPlayer().setName(characterName);
                CampaignManager.applyStartingQuests();
                if (!WorldSave.saveInitialSnapshot()) {
                    Forge.clearTransitionScreen();
                    StartScene menu = StartScene.instance();
                    Forge.switchScene(menu);
                    menu.showError(Forge.getLocalizer().getMessageorUseDefault("lblInitialSaveFailed",
                            "Your new playthrough is active, but its first save failed. Resume or try saving again.")
                            + "\n" + WorldSave.getLastError());
                    return;
                }
                SoundSystem.instance.changeBackgroundTrack();
                WorldStage.getInstance().enterSpawnPOI();
                Forge.switchScene(GameScene.instance());
            } catch (Exception e) {
                String error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                WorldSave.discardFailedPlaythrough();
                if (WorldSave.canContinue())
                    WorldSave.loadContinue();
                Forge.clearTransitionScreen();
                StartScene menu = StartScene.instance();
                Forge.switchScene(menu);
                menu.showError(error);
            } finally {
                started = false;
            }
        };
        Forge.setTransitionScreen(new TransitionScreen(runnable, null, false, true,
                Forge.getLocalizer().getMessage("lblGeneratingWorld")));
    }

    public boolean back() {
        Forge.switchScene(CampaignSelectionScene.instance());
        return true;
    }


    private void rightAvatar() {

        avatarIndex++;
        updateAvatar();
    }

    private void leftAvatar() {
        avatarIndex--;
        updateAvatar();
    }

    private boolean updateAvatar() {
        avatarImage.setDrawable(new TextureRegionDrawable(previewHeroes.getAvatar(race.getCurrentIndex(), gender.getCurrentIndex() != 0, avatarIndex)));
        return false;
    }


    @Override
    public void enter() {
        updateAvatar();

        unselectActors();
        super.enter();
    }

    private void showDifficultyHelp() {
        Localizer localizer = Forge.getLocalizer();
        DifficultyData selectedDifficulty = previewConfig.getConfigData().difficulties[difficulty.getCurrentIndex()];
        boolean enableGeneticAI = previewConfig.getConfigData().enableGeneticAI;
        String startingEquipment = selectedDifficulty.startItems == null || selectedDifficulty.startItems.length == 0
                ? localizer.getMessage("lblNone")
                : String.join(", ", selectedDifficulty.startItems);

        difficultySummary = new DialogData();
        difficultySummary.name = localizer.getMessage("lblSummary");
        switch (selectedDifficulty.name) {
            case "Easy":
                difficultySummary.text = localizer.getMessage("advDifficultySummaryEasy", selectedDifficulty.name, startingEquipment);
                break;
            case "Normal":
                difficultySummary.text = localizer.getMessage("advDifficultySummaryNormal", selectedDifficulty.name, startingEquipment);
                break;
            case "Hard":
                if (enableGeneticAI) {
                    difficultySummary.text = localizer.getMessage("advDifficultySummaryHardGenetic", selectedDifficulty.name, startingEquipment);
                } else {
                    difficultySummary.text = localizer.getMessage("advDifficultySummaryHard", selectedDifficulty.name, startingEquipment);
                }
                break;
            case "Insane":
                difficultySummary.text = localizer.getMessage("advDifficultySummaryInsane", selectedDifficulty.name, startingEquipment);
                break;
            default:
                difficultySummary.text = localizer.getMessage("advDifficultySummaryCustom");
                break;
        }


        DialogData dismiss = new DialogData();
        dismiss.name = localizer.getMessage("lblOK");

        DialogData matchImpacts = new DialogData();
        matchImpacts.text = localizer.getMessage("advDifficultyMatchImpacts", selectedDifficulty.name, selectedDifficulty.startingLife, (int) (selectedDifficulty.enemyLifeFactor * 100), (int) (selectedDifficulty.goldLoss * 100), (int) (selectedDifficulty.lifeLoss * 100));
        matchImpacts.name = localizer.getMessage("lblDuels");

        DialogData economyImpacts = new DialogData();
        economyImpacts.text = localizer.getMessage("advDifficultyEconomyImpacts", selectedDifficulty.name, selectedDifficulty.startingMoney, selectedDifficulty.startingShards, (int) (selectedDifficulty.sellFactor * 100), (int) (selectedDifficulty.shardSellRatio * 100), (int) (selectedDifficulty.rewardMaxFactor * 100));
        economyImpacts.name = localizer.getMessage("lblEconomy");

        difficultySummary.options = new DialogData[3];
        difficultySummary.options[0] = matchImpacts;
        difficultySummary.options[1] = economyImpacts;
        difficultySummary.options[2] = dismiss;
        matchImpacts.options = new DialogData[3];
        matchImpacts.options[0] = difficultySummary;
        matchImpacts.options[1] = economyImpacts;
        matchImpacts.options[2] = dismiss;
        economyImpacts.options = new DialogData[3];
        economyImpacts.options[0] = difficultySummary;
        economyImpacts.options[1] = matchImpacts;
        economyImpacts.options[2] = dismiss;

        loadDialog(difficultySummary);
    }

    private void showModeHelp() {

        Localizer localizer = Forge.getLocalizer();
        AdventureModes selectedMode = modes.get(mode.getCurrentIndex());
        DifficultyData selectedDifficulty = previewConfig.getConfigData().difficulties[difficulty.getCurrentIndex()];
        boolean enableGeneticAI = previewConfig.getConfigData().enableGeneticAI;

        modeSummary = new DialogData();
        modeSummary.name = localizer.getMessage("lblSummary");

        StringBuilder summaryText = new StringBuilder();
        switch (selectedMode) {
            case Standard:
                summaryText.append(localizer.getMessage("advModeStandardSummary"));
                switch (selectedDifficulty.name) {
                    case "Easy":
                        summaryText.append(localizer.getMessage("advDiffEasyStandard"));
                        break;
                    case "Normal":
                        summaryText.append(localizer.getMessage("advDiffNormalStandard"));
                        break;
                    case "Hard":
                        summaryText.append(localizer.getMessage("advDiffHardStandard"));
                        break;
                    case "Insane":
                        summaryText.append(localizer.getMessage("advDiffInsaneStandard"));
                        break;
                    default:
                        difficultySummary.text = localizer.getMessage("advCannotDetermineStarterDeck");
                        break;
                }
                break;
            case Constructed:
                summaryText.append(localizer.getMessage("advModeConstructedSummary"));
                switch (selectedDifficulty.name) {
                    case "Easy":
                        summaryText.append(localizer.getMessage("advDiffEasyConstructed"));
                        break;
                    case "Normal":
                        summaryText.append(localizer.getMessage("advDiffNormalConstructed"));
                        break;
                    case "Hard":
                        summaryText.append(localizer.getMessage("advDiffHardConstructed"));
                        break;
                    case "Insane":
                        summaryText.append(localizer.getMessage("advDiffInsaneConstructed"));
                        break;
                    default:
                        difficultySummary.text = localizer.getMessage("advCannotDetermineStarterDeck");
                        break;
                }
                break;
            case Pile:
                summaryText.append(localizer.getMessage("advModePileSummary"));
                switch (selectedDifficulty.name) {
                    case "Easy":
                        summaryText.append(localizer.getMessage("advDiffEasyPile"));
                        break;
                    case "Normal":
                        summaryText.append(localizer.getMessage("advDiffNormalPile"));
                        break;
                    case "Hard":
                        summaryText.append(localizer.getMessage("advDiffHardPile"));
                        summaryText.append(localizer.getMessage("advPileLessRareReward"));
                        break;
                    case "Insane":
                        summaryText.append(localizer.getMessage("advDiffInsanePile"));
                        summaryText.append(localizer.getMessage("advPileLessRareReward"));
                        break;
                    default:
                        difficultySummary.text = localizer.getMessage("advCannotDetermineStarterDeck");
                        break;
                }
                break;
            case Chaos:
                summaryText.append(localizer.getMessage("advModeChaosSummary"));
                break;
            case Custom:
                if (enableGeneticAI) {
                    summaryText.append(localizer.getMessage("advModeCustomGeneticSummary"));
                } else {
                    summaryText.append(localizer.getMessage("advModeCustomSummary"));
                }
                break;
            case Precon:
                summaryText.append(localizer.getMessage("advModePreconSummary"));
                break;
            case Commander:
                summaryText.append(localizer.getMessage("advModeCommanderSummary"));
                break;
            case CommanderPrecon:
                summaryText.append(localizer.getMessage("advModeCommanderPreconSummary"));
                break;
            default:
                summaryText.append(localizer.getMessage("advNoModeSummaryAvailable"));
                break;
        }

        DialogData dismiss = new DialogData();
        dismiss.name = localizer.getMessage("lblOK");
        modeSummary.text = summaryText.toString();
        modeSummary.options = new DialogData[1];
        modeSummary.options[0] = dismiss;
        loadDialog(modeSummary);
    }
}
