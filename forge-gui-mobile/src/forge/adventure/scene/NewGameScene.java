package forge.adventure.scene;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.github.tommyettinger.textra.TextraButton;
import forge.Forge;
import forge.adventure.campaign.CampaignDefinition;
import forge.adventure.campaign.CampaignManager;
import forge.adventure.character.*;
import forge.adventure.data.CharacterCreationData.*;
import forge.adventure.data.DifficultyData;
import forge.adventure.data.HeroListData;
import forge.adventure.stage.WorldStage;
import forge.adventure.util.*;
import forge.adventure.world.WorldSave;
import forge.card.CardEdition;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.deck.io.DeckSerializer;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.screens.TransitionScreen;
import forge.sound.SoundSystem;
import forge.util.NameGenerator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Campaign-local creation preview. Activating a campaign remains part of the existing playthrough transaction. */
public class NewGameScene extends MenuScene {
    private final CampaignDefinition campaign;
    private final Config previewConfig;
    private final HeroListData previewHeroes;
    private final CharacterCreationCatalog catalog;
    private final TextField selectedName;
    private final Selector gender = selector("gender"), race = selector("race"), difficulty = selector("difficulty");
    private final Selector portraits = selector("portraits"), mode = selector("starterMode");
    private final Selector edition = selector("starterEdition"), deckChoice = selector("starterDeck");
    private final CharacterImageSelector appearance = new CharacterImageSelector();
    private final CheckBox matchRace = Controls.newCheckBox(text("lblMatchPortraits", "Match portraits"));
    private final CheckBox matchGender = Controls.newCheckBox(text("lblMatchPortraits", "Match portraits"));
    private final Image avatarImage = new Image();
    private final Label portraitFeedback = Controls.newLabel("");
    private final Label deckSummary = Controls.newLabel("");
    private final TextraButton startButton;
    private final List<AdventureModes> modes = new ArrayList<>();
    private final List<CardEdition> editions = new ArrayList<>();
    private List<Portrait> visiblePortraits = List.of();
    private Portrait selectedPortrait;
    private byte[] importedPortrait = new byte[0];
    private PortraitImages.Handle portraitHandle;
    private boolean rebuilding, started, disposed;
    private DeckState workingDeck, confirmedDeck;
    private boolean needsDeckReview;
    private boolean appearanceValid, appearanceError;
    private String deckError = "";
    private Dialog deckWindow;
    private PortraitImportDialog portraitDialog;
    private Table cardFrame, cardList;
    private Selector cardChoice;
    private Table editionField;
    private Cell<?> editionCell;
    private RewardActor previewActor;
    private final List<PaperCard> previewCards = new ArrayList<>();
    private static NewGameScene object;

    private record DeckState(Deck deck, AdventureModes mode, ColorSet color, int modeIndex, int deckIndex,
                             int editionIndex, CardEdition edition, String label, String classId, String source) { }

    private NewGameScene(CampaignDefinition campaign, Config config) {
        super(Forge.isLandscapeMode() ? "ui/new_game.json" : "ui/new_game_portrait.json", config);
        this.campaign = campaign; previewConfig = config; previewHeroes = HeroListData.forConfig(config);
        catalog = new CharacterCreationCatalog(config.getConfigData().characterCreation, previewHeroes.getHeroes(),
                previewHeroes.creationPortraits());
        selectedName = Controls.newTextField(""); selectedName.setName("nameField"); selectedName.setMaxLength(60);
        startButton = Controls.newTextButton(text("lblBeginJourney", "Begin journey"), this::start);
        startButton.setName("start");
        appearance.setName("appearance"); avatarImage.setName("avatarPreview"); avatarImage.setScaling(Scaling.fit);
        portraitFeedback.setName("portraitFeedback"); portraitFeedback.setWrap(true);
        matchRace.setName("matchPortraitRace"); matchGender.setName("matchPortraitGender");
        matchRace.setChecked(catalog.settings.matchPortraitRace); matchGender.setChecked(catalog.settings.matchPortraitGender);
        gender.setTextList(catalog.genders.stream().map(g -> label(g)).toArray(String[]::new));
        race.setTextList(catalog.races.stream().map(r -> label(r)).toArray(String[]::new));
        appearance.setTextList(catalog.appearances.stream().map(a -> "").toArray(String[]::new));
        int human = 0;
        for (int i = 0; i < catalog.races.size(); i++) if ("human".equals(catalog.races.get(i).id)) human = i;
        race.setCurrentIndex(human);
        String startingRace = catalog.races.get(human).id;
        for (int i = 0; i < catalog.appearances.size(); i++) {
            if (catalog.appearances.get(i).id.startsWith(startingRace + "-")) { appearance.setCurrentIndex(i); break; }
        }
        difficulty.setTextList(java.util.Arrays.stream(config.getConfigData().difficulties)
                .map(d -> text("lbl" + d.name, d.name)).toArray(String[]::new));
        for (int i = 0; i < config.getConfigData().difficulties.length; i++)
            if (config.getConfigData().difficulties[i].startingDifficulty) difficulty.setCurrentIndex(i);
        selectedName.setText(randomName(catalog.genders.get(0)));
        onChange(gender, this::refreshPortraits); onChange(race, this::refreshPortraits);
        onChange(matchGender, this::refreshPortraits); onChange(matchRace, this::refreshPortraits);
        onChange(appearance, this::updateAppearance);
        onChange(portraits, () -> {
            if (rebuilding || visiblePortraits.isEmpty()) return;
            loadPortrait(visiblePortraits.get(portraits.getCurrentIndex()), new byte[0]);
        });
        onChange(difficulty, this::changeDifficulty);
        onChange(mode, () -> { if (!rebuilding) { rebuildDeckChoices(); loadWorkingDeck(); } });
        onChange(edition, () -> { if (!rebuilding) { rebuildDeckChoices(); loadWorkingDeck(); } });
        onChange(deckChoice, () -> {
            if (!rebuilding && !(workingDeck != null && selectedMode() == AdventureModes.Chaos && workingDeck.mode == AdventureModes.Chaos))
                loadWorkingDeck();
        });
        rebuildDeckModes(); loadWorkingDeck(); confirmedDeck = workingDeck;
        buildLayout(); refreshPortraits(); updateAppearance(); updateMainDeck();
    }

    public static NewGameScene instance() { return object == null ? forCampaign(CampaignManager.current()) : object; }
    public static NewGameScene forCampaign(CampaignDefinition campaign) {
        NewGameScene next = new NewGameScene(campaign, Config.forCampaign(campaign));
        Forge.retireAdventureResource(object); object = next; return next;
    }
    public static void resetForCampaign() { Forge.retireAdventureResource(object); object = null; }

    private static String text(String key, String fallback) { return Forge.getLocalizer().getMessageorUseDefault(key, fallback); }
    private static String label(Option option) { return UIActor.localize(option.label == null || option.label.isEmpty() ? option.id : option.label); }
    private static String randomName(Gender gender) {
        return forge.util.MyRandom.withRandom(new java.util.Random(), () -> NameGenerator.getRandomName(gender.namePool, "Any", ""));
    }
    private static Selector selector(String name) {
        Selector selector = new CharacterOptionSelector(); selector.setName(name);
        selector.addCaptureListener(new InputListener() {
            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (selector.getStage() != null) selector.getStage().setKeyboardFocus(selector);
                return false;
            }
        });
        return selector;
    }
    private static void onChange(Actor actor, Runnable action) {
        actor.addListener(new ChangeListener() { @Override public void changed(ChangeEvent event, Actor actor) { action.run(); } });
    }
    private static void singletonArrows(Selector selector, int count) {
        selector.getLeftArrow().setDisabled(count <= 1); selector.getRightArrow().setDisabled(count <= 1);
    }
    private void selectControl(Actor control) { addToSelectable(control); }
    private Table field(String title, Actor control, float width, float height) {
        Table field = new Table(); field.add(Controls.newLabel(title)).left().width(width).height(11).row();
        field.add(control).width(width).height(height); return field;
    }
    private Table identityField(String title, Selector selector, CheckBox match, float width) {
        Table heading = new Table(); heading.add(Controls.newLabel(title)).left().expandX();
        match.getLabel().setFontScale(.75f); heading.add(match).right();
        Table field = new Table(); field.add(heading).width(width).height(13).row(); field.add(selector).width(width).height(19);
        return field;
    }

    private void buildLayout() {
        ui.clearChildren(); clearSelectable();
        float width = stage.getViewport().getWorldWidth(), height = stage.getViewport().getWorldHeight();
        ui.setSize(width, height);
        Table root = new Table(); root.setName("characterCreationRoot"); root.setFillParent(true); root.pad(9);
        Window.WindowStyle windowStyle = Controls.getSkin().get(Window.WindowStyle.class);
        root.setBackground(windowStyle.background);
        Label campaignTitle = Controls.newLabel(campaign.getTitle()); campaignTitle.setName("campaignTitle"); campaignTitle.setFontScale(1.35f);
        TextraButton back = Controls.newTextButton(text("lblBack", "Back"), this::back); back.setName("back");
        Table header = new Table(); header.add(campaignTitle).left().expandX().fillX(); header.add(back).width(48).height(18);
        root.add(header).expandX().fillX().height(22).padBottom(5).row();
        boolean wide = width > height;
        float leftWidth = wide ? Math.min(140, width * .31f) : Math.min(160, width - 30);
        ScrollPane.ScrollPaneStyle scrollStyle = Controls.getSkin().get(ScrollPane.ScrollPaneStyle.class);
        float scrollPadding = scrollStyle.background == null ? 0 : scrollStyle.background.getLeftWidth() + scrollStyle.background.getRightWidth();
        float rightWidth = wide ? width - leftWidth - 34 - scrollPadding : width - 26 - scrollPadding;
        float portraitSize = Math.min(leftWidth, wide ? 120 : 144);
        Table left = new Table(); left.top();
        left.add(avatarImage).width(portraitSize).height(portraitSize).padBottom(3).row();
        left.add(portraits).width(leftWidth).height(19).padBottom(4).row();
        TextraButton importButton = Controls.newTextButton(text("lblImportImage", "Import image…"), this::choosePortraitFile);
        importButton.setName("importPortrait"); importButton.setVisible(catalog.settings.allowPortraitImport);
        if (catalog.settings.allowPortraitImport) left.add(importButton).width(leftWidth).height(19).padBottom(4).row();
        left.add(startButton).width(leftWidth).height(19).padBottom(3).row();
        left.add(portraitFeedback).width(leftWidth).padBottom(2);

        Table right = new Table(); right.top();
        right.add(field(text("lblName", "Name"), selectedName, rightWidth, 19)).padBottom(5).row();
        Table identity = new Table();
        identity.add(identityField(text("lblGender", "Gender"), gender, matchGender, (rightWidth - 10) / 2)).padRight(10);
        identity.add(identityField(text("lblRace", "Race"), race, matchRace, (rightWidth - 10) / 2));
        right.add(identity).padBottom(7).row();
        right.add(field(text("lblInWorldAppearance", "In-world appearance"), appearance, rightWidth, 32)).padBottom(7).row();
        Table starter = new Table(); deckSummary.setName("starterSummary"); deckSummary.setWrap(true);
        starter.add(deckSummary).width(Math.max(80, rightWidth - 82)).left().padRight(5);
        TextraButton choose = Controls.newTextButton(catalog.starterClasses.isEmpty() ? text("lblChooseDeck", "Choose deck…")
                : text("lblChooseClass", "Choose class…"), this::showDeckWindow); choose.setName("chooseStarter");
        starter.add(choose).width(77).height(20);
        right.add(field(catalog.starterClasses.isEmpty() ? text("lblDeck", "Starting deck") : text("lblClass", "Class"), starter, rightWidth, 30)).padBottom(6).row();
        right.add(field(text("lblDifficulty", "Difficulty"), difficulty, rightWidth, 19));

        Table content = new Table(); content.top();
        if (wide) { content.add(left).width(leftWidth).top().padRight(16); content.add(right).width(rightWidth).top(); }
        else { content.add(left).width(leftWidth).padBottom(12).row(); content.add(right).width(rightWidth); }
        ScrollPane scroll = new ScrollPane(content, Controls.getSkin()); scroll.setScrollingDisabled(true, false); scroll.setFadeScrollBars(false);
        root.add(scroll).expand().fill(); ui.addActor(root); stage.setScrollFocus(scroll);
        selectControl(selectedName); selectControl(gender); selectControl(matchGender); selectControl(race); selectControl(matchRace);
        selectControl(appearance); selectControl(portraits); if (catalog.settings.allowPortraitImport) selectControl(importButton);
        selectControl(choose); selectControl(difficulty); selectControl(startButton); selectControl(back);
        singletonArrows(gender, catalog.genders.size()); singletonArrows(race, catalog.races.size());
        singletonArrows(appearance, catalog.appearances.size()); singletonArrows(difficulty, previewConfig.getConfigData().difficulties.length);
    }

    private void refreshPortraits() {
        if (rebuilding) return;
        String prior = selectedPortrait == null ? "" : selectedPortrait.id;
        visiblePortraits = catalog.matchingPortraits(catalog.races.get(race.getCurrentIndex()).id,
                catalog.genders.get(gender.getCurrentIndex()).id, matchRace.isChecked(), matchGender.isChecked());
        rebuilding = true;
        portraits.setTextList(visiblePortraits.isEmpty() ? new String[]{text("lblNoMatches", "No matches")}
                : visiblePortraits.stream().map(NewGameScene::label).toArray(String[]::new));
        int selected = 0;
        for (int i = 0; i < visiblePortraits.size(); i++) if (visiblePortraits.get(i).id.equals(prior)) selected = i;
        portraits.setCurrentIndex(selected); rebuilding = false;
        if (importedPortrait.length > 0) portraits.getLabel().setText(text("lblYourPortrait", "Your portrait"));
        else if (visiblePortraits.isEmpty()) {
            selectedPortrait = null; avatarImage.setDrawable(null);
            retirePortrait(); portraitFeedback.setText(text("lblNoPortraitMatches", "No matching portraits. Change the match options or import an image."));
        } else if (portraitHandle == null || !visiblePortraits.get(selected).id.equals(prior))
            loadPortrait(visiblePortraits.get(selected), new byte[0]);
        else portraitFeedback.setText("");
        boolean disabled = visiblePortraits.isEmpty() || visiblePortraits.size() == 1 && importedPortrait.length == 0;
        portraits.getLeftArrow().setDisabled(disabled); portraits.getRightArrow().setDisabled(disabled); updateStart();
    }
    private void loadPortrait(Portrait portrait, byte[] imported) {
        try {
            PortraitImages.Handle next = PortraitImages.load(previewConfig, previewHeroes, portrait, imported);
            retirePortrait(); portraitHandle = next; selectedPortrait = portrait; importedPortrait = imported.clone();
            avatarImage.setDrawable(new TextureRegionDrawable(next.region)); portraitFeedback.setText(""); updateStart();
        } catch (RuntimeException error) {
            selectedPortrait = null; avatarImage.setDrawable(null); retirePortrait(); portraitFeedback.setText(error.getMessage()); updateStart();
        }
    }
    private void retirePortrait() { if (portraitHandle != null) Forge.retireAdventureResource(portraitHandle); portraitHandle = null; }
    private void updateAppearance() {
        try {
            Appearance selected = catalog.appearances.get(appearance.getCurrentIndex());
            PortraitImages.resource(previewConfig, selected.sprite);
            TextureAtlas atlas = previewConfig.getAtlas(selected.sprite);
            TextureAtlas.AtlasRegion idle = atlas.findRegion("IdleDown");
            if (idle == null) idle = atlas.findRegion("IdleRightDown");
            if (idle == null) idle = atlas.getRegions().first();
            appearance.setRegion(idle); appearanceValid = true;
            if (appearanceError) { appearanceError = false; refreshPortraits(); }
        } catch (RuntimeException error) {
            appearanceValid = false; appearanceError = true; appearance.clearRegion();
            portraitFeedback.setText("Appearance unavailable. Choose another appearance.");
        }
        updateStart();
    }
    private void choosePortraitFile() {
        if (portraitDialog != null || disposed) return;
        portraitDialog = new PortraitImportDialog(stage.getWidth(), stage.getHeight(), this::importPortrait, this::closePortraitDialog);
        showDialog(portraitDialog); clearSelectable();
        selectControl(portraitDialog.pathField); selectControl(portraitDialog.files);
        selectControl(portraitDialog.up); selectControl(portraitDialog.roots);
        addToSelectable(portraitDialog.getButtonTable()); enableDialogNavigation();
        stage.setKeyboardFocus(portraitDialog.pathField);
    }
    private void closePortraitDialog() {
        if (portraitDialog != null) { removeDialog(); portraitDialog = null; stage.setKeyboardFocus(ui.findActor("importPortrait")); }
    }
    /** Same render-thread import path used by the picker and the native save-portability check. */
    public void importPortrait(Path file) throws java.io.IOException {
        if (disposed || !catalog.settings.allowPortraitImport) throw new IllegalStateException("Portrait import is unavailable.");
        byte[] png = PortraitImages.importImage(file);
        PortraitImages.Handle next = PortraitImages.load(previewConfig, previewHeroes, null, png);
        Portrait imported = new Portrait(); imported.id = "imported"; imported.label = text("lblYourPortrait", "Your portrait");
        retirePortrait(); portraitHandle = next; selectedPortrait = imported; importedPortrait = png;
        avatarImage.setDrawable(new TextureRegionDrawable(next.region)); portraitFeedback.setText(""); refreshPortraits();
    }

    private DifficultyData selectedDifficulty() { return previewConfig.getConfigData().difficulties[difficulty.getCurrentIndex()]; }
    private boolean curatedClasses() { return !catalog.starterClasses.isEmpty(); }
    private AdventureModes selectedMode() {
        return curatedClasses() ? AdventureModes.valueOf(catalog.starterClasses.get(deckChoice.getCurrentIndex()).mode)
                : modes.get(mode.getCurrentIndex());
    }
    private void rebuildDeckModes() {
        rebuildDeckModes(AdventureModes.Constructed);
    }
    private void rebuildDeckModes(AdventureModes preferred) {
        rebuilding = true; modes.clear(); DifficultyData diff = selectedDifficulty();
        if (diff.starterDecks != null) modes.add(AdventureModes.Standard);
        if (diff.constructedStarterDecks != null) modes.add(AdventureModes.Constructed);
        if (diff.pileDecks != null) modes.add(AdventureModes.Pile);
        if (diff.commanderDecks != null) modes.add(AdventureModes.Commander);
        if (previewConfig.hasPreconDecks()) modes.add(AdventureModes.Precon);
        if (previewConfig.hasCommanderPreconDecks()) modes.add(AdventureModes.CommanderPrecon);
        modes.add(AdventureModes.Chaos);
        if (DeckProxy.getAllCustomStarterDecks().iterator().hasNext()) modes.add(AdventureModes.Custom);
        modes.removeIf(value -> !previewConfig.getConfigData().allowsStarterMode(value.name()));
        if (!curatedClasses() && modes.isEmpty()) throw new IllegalArgumentException("This campaign has no supported starter modes.");
        mode.setTextList(modes.isEmpty() ? new String[]{""} : modes.stream().map(AdventureModes::getName).toArray(String[]::new));
        int preferredIndex = modes.indexOf(preferred);
        mode.setCurrentIndex(preferredIndex >= 0 ? preferredIndex : Math.max(0, modes.indexOf(AdventureModes.Constructed)));
        rebuilding = false; rebuildDeckChoices();
    }
    private void rebuildDeckChoices() {
        rebuilding = true;
        if (curatedClasses()) {
            deckChoice.setTextList(catalog.starterClasses.stream().map(NewGameScene::label).toArray(String[]::new));
            edition.setTextList(new String[]{""}); editions.clear();
        } else {
            AdventureModes selected = selectedMode();
            int priorEdition = edition.getCurrentIndex(); editions.clear();
            if (selected == AdventureModes.Precon) edition.setTextList(previewConfig.getPreconSetNames());
            else if (selected == AdventureModes.CommanderPrecon) edition.setTextList(previewConfig.getCommanderPreconSetNames());
            else {
                edition.setTextList(previewConfig.starterEditionNames());
                for (String code : previewConfig.starterEditions()) editions.add(FModel.getMagicDb().getEditions().get(code));
            }
            edition.setCurrentIndex(Math.min(priorEdition, Math.max(0, edition.getTextList().size - 1)));
            if (selected == AdventureModes.Precon) deckChoice.setTextList(previewConfig.filterPreconDecks(edition.getCurrentIndex()));
            else if (selected == AdventureModes.CommanderPrecon) deckChoice.setTextList(previewConfig.filterCommanderPreconDecks(edition.getCurrentIndex()));
            else if (selected == AdventureModes.Custom) {
                List<String> names = new ArrayList<>(); for (DeckProxy deck : DeckProxy.getAllCustomStarterDecks()) names.add(deck.getName());
                deckChoice.setTextList(names.toArray(String[]::new));
            } else if (selected == AdventureModes.Chaos) deckChoice.setTextList(new String[]{text("lblRandomDeck", "Random deck")});
            else deckChoice.setTextList(previewConfig.colorIdNames());
        }
        singletonArrows(mode, modes.size()); singletonArrows(edition, edition.getTextList().size);
        singletonArrows(deckChoice, deckChoice.getTextList().size); rebuilding = false;
        updateEditionVisibility();
    }
    private void updateEditionVisibility() {
        if (editionField == null) return;
        boolean visible = selectedMode().usesStarterEditionSelector();
        editionField.setVisible(visible); edition.setVisible(visible); editionCell.height(visible ? 28 : 0);
        if (deckWindow != null && deckWindow.getStage() != null) {
            deckWindow.pack(); deckWindow.setPosition((stage.getWidth() - deckWindow.getWidth()) / 2,
                    (stage.getHeight() - deckWindow.getHeight()) / 2);
        }
    }
    private CardEdition selectedEdition() {
        if (editions.isEmpty()) return null;
        return editions.get(Math.min(edition.getCurrentIndex(), editions.size() - 1));
    }
    private String deckSource(AdventureModes selected, ColorSet color, int index) {
        if (curatedClasses()) return catalog.starterClasses.get(index).mode + "|" + catalog.starterClasses.get(index).deck;
        if (selected.usesFolderDeckPicker()) return selected.name() + "|" + edition.getCurrentIndex() + "|" + edition.getText() + "|" + index;
        return previewConfig.starterDeckSourceKey(color, selectedDifficulty(), selected, index, selectedEdition());
    }
    private ColorSet selectionColor() {
        int index = deckChoice.getCurrentIndex();
        String[] colors = previewConfig.colorIds();
        String color = curatedClasses() ? catalog.starterClasses.get(index).color : colors[index < colors.length ? index : 0];
        return ColorSet.fromNames((color == null ? "" : color).toCharArray());
    }
    private void changeDifficulty() {
        if (rebuilding) return;
        DeckState prior = confirmedDeck;
        if (curatedClasses()) { updateMainDeck(); return; }
        AdventureModes preferred = prior == null ? selectedMode() : prior.mode;
        rebuildDeckModes(preferred);
        if (prior != null && modes.contains(prior.mode)) {
            rebuilding = true; edition.setCurrentIndex(prior.editionIndex); rebuilding = false;
            rebuildDeckChoices();
            rebuilding = true; deckChoice.setCurrentIndex(prior.deckIndex); rebuilding = false;
            if (prior.source.equals(deckSource(prior.mode, prior.color, deckChoice.getCurrentIndex()))) {
                workingDeck = confirmedDeck = new DeckState(prior.deck, prior.mode, prior.color, mode.getCurrentIndex(),
                        deckChoice.getCurrentIndex(), edition.getCurrentIndex(), selectedEdition(), prior.label, prior.classId, prior.source);
                needsDeckReview = false; updateMainDeck(); return;
            }
        }
        loadWorkingDeck(); confirmedDeck = null; needsDeckReview = true; updateMainDeck();
    }
    private void loadWorkingDeck() {
        try {
            workingDeck = StarterDeckPreview.generate(previewConfig, selectedDifficulty(), selectedMode(), selectionColor(), this::buildWorkingDeck);
            deckError = "";
        } catch (RuntimeException error) {
            workingDeck = null; deckError = error.getMessage();
            System.err.println("Starting deck preview: " + deckError);
        }
        if (deckWindow != null) rebuildCardPreview();
    }
    private DeckState buildWorkingDeck() {
            AdventureModes selected = selectedMode(); int index = deckChoice.getCurrentIndex();
            ColorSet color; Deck deck; String classId = "", name;
            if (curatedClasses()) {
                StarterClass starter = catalog.starterClasses.get(index);
                if (!previewConfig.getConfigData().allowsStarterMode(selected.name())) throw new IllegalArgumentException("Class uses a disabled starter mode.");
                color = selectionColor();
                deck = DeckSerializer.fromFile(PortraitImages.resource(previewConfig, starter.deck).file());
                classId = starter.id; name = label(starter);
            } else {
                String[] colors = previewConfig.colorIds();
                color = ColorSet.fromNames(colors[index < colors.length ? index : 0].toCharArray());
                deck = previewConfig.starterDeck(color, selectedDifficulty(), selected, index, selectedEdition());
                name = UIActor.localize(deckChoice.getText());
            }
            if (deck == null || deck.getMain().isEmpty()) throw new IllegalArgumentException("This starting deck contains no cards.");
            return new DeckState(new Deck(deck), selected, color, mode.getCurrentIndex(), index,
                    edition.getCurrentIndex(), selectedEdition(), name, classId, deckSource(selected, color, index));
    }
    private static String colorName(ColorSet color) {
        List<String> names = new ArrayList<>(); byte bits = color.getColor();
        if ((bits & MagicColor.WHITE) != 0) names.add(text("lblWhite", "White"));
        if ((bits & MagicColor.BLUE) != 0) names.add(text("lblBlue", "Blue"));
        if ((bits & MagicColor.BLACK) != 0) names.add(text("lblBlack", "Black"));
        if ((bits & MagicColor.RED) != 0) names.add(text("lblRed", "Red"));
        if ((bits & MagicColor.GREEN) != 0) names.add(text("lblGreen", "Green"));
        return names.isEmpty() ? text("lblColorless", "Colorless") : String.join("/", names);
    }
    private String deckDetails(DeckState state) {
        return colorName(DeckProxy.getColorIdentity(state.deck)) + " · " + state.deck.getAllCardsInASinglePool(true, true).countAll()
                + " " + text("lblCards", "cards");
    }
    private void updateMainDeck() {
        deckSummary.setText(confirmedDeck == null ? (needsDeckReview ? text("lblReviewStarterForDifficulty", "Choose a starting deck for this difficulty.")
                : deckError.isEmpty() ? text("lblChooseDeck", "Choose deck…") : text("lblNoDeck", "Starting deck unavailable"))
                : confirmedDeck.label + "\n" + deckDetails(confirmedDeck)); updateStart();
    }
    private void updateStart() { startButton.setDisabled(!appearanceValid || selectedPortrait == null || portraitHandle == null || confirmedDeck == null); }

    private void showDeckWindow() {
        if (deckWindow != null) return;
        DeckState prior = confirmedDeck;
        deckWindow = Controls.newDialog(curatedClasses() ? text("lblChooseClass", "Choose your class") : text("lblChooseDeck", "Choose your deck"));
        TextraButton confirm = Controls.newTextButton(text("lblUseThisDeck", "Use this deck"), () -> {
                    if (workingDeck == null) return;
                    confirmedDeck = workingDeck; needsDeckReview = false; removeDialog(); closeDeckWindow(); updateMainDeck();
                });
        confirm.setName("confirmStarter"); confirm.setDisabled(workingDeck == null);
        TextraButton cancel = Controls.newTextButton(text("lblCancel", "Cancel"), () -> { restoreDeck(prior); removeDialog(); closeDeckWindow(); });
        cancel.setName("cancelStarter");
        deckWindow.getButtonTable().add(confirm).pad(2).height(20); deckWindow.getButtonTable().add(cancel).pad(2).height(20);
        float width = stage.getViewport().getWorldWidth() - 36;
        Table content = deckWindow.getContentTable();
        if (!curatedClasses()) {
            content.add(field(text("lblMode", "Mode"), mode, width, 17)).colspan(2).row();
            editionField = field(text("lblEdition", "Edition"), edition, width, 17);
            editionCell = content.add(editionField).colspan(2); content.row(); updateEditionVisibility();
        }
        content.add(deckChoice).colspan(2).width(width).height(19).padBottom(3).row();
        Label details = Controls.newLabel(""); details.setName("deckDetails");
        content.add(details).colspan(2).height(12).padBottom(3).row();
        float cardHeight = Math.max(72, Math.min(178, stage.getViewport().getWorldHeight() - (curatedClasses() ? 144 : 208)));
        float cardWidth = cardHeight * 488f / 680f;
        cardFrame = new Table(); cardFrame.setName("deckCardFrame"); cardFrame.setSize(cardWidth, cardHeight);
        cardList = new Table(); cardList.top().left();
        ScrollPane list = new ScrollPane(cardList, Controls.getSkin()); list.setScrollingDisabled(true, false);
        content.add(list).width(width - cardWidth - 8).height(cardHeight).top().padRight(8);
        content.add(cardFrame).width(cardWidth).height(cardHeight).row();
        cardChoice = selector("cardChoice"); onChange(cardChoice, this::showCard);
        content.add().width(width - cardWidth - 8);
        content.add(cardChoice).width(cardWidth).height(17);
        rebuildCardPreview(); showDialog(deckWindow);
        clearSelectable(); if (!curatedClasses()) { selectControl(mode); selectControl(edition); }
        selectControl(deckChoice); selectControl(cardChoice); addToSelectable(deckWindow.getButtonTable()); enableDialogNavigation();
        stage.setKeyboardFocus(curatedClasses() ? deckChoice : mode);
    }
    private void restoreDeck(DeckState prior) {
        if (prior == null) return;
        rebuilding = true; mode.setCurrentIndex(prior.modeIndex); rebuilding = false; rebuildDeckChoices();
        rebuilding = true; edition.setCurrentIndex(prior.editionIndex); rebuilding = false; rebuildDeckChoices();
        rebuilding = true; deckChoice.setCurrentIndex(prior.deckIndex); rebuilding = false;
        workingDeck = prior;
    }
    private void rebuildCardPreview() {
        if (cardList == null) return;
        cardList.clear(); previewCards.clear();
        Label details = deckWindow.findActor("deckDetails");
        if (details != null) details.setText(workingDeck == null ? text("lblNoDeck", "No deck available") : deckDetails(workingDeck));
        Button confirm = deckWindow.findActor("confirmStarter"); if (confirm != null) confirm.setDisabled(workingDeck == null);
        if (workingDeck != null) {
            for (Map.Entry<PaperCard, Integer> entry : workingDeck.deck.getAllCardsInASinglePool(true, true)) {
                int index = previewCards.size(); previewCards.add(entry.getKey());
                TextraButton button = Controls.newTextButton(entry.getValue() + "× " + entry.getKey().getName(), () -> cardChoice.setCurrentIndex(index));
                button.setName("starterCard-" + index); cardList.add(button).expandX().fillX().height(17).row();
            }
        }
        cardChoice.setTextList(previewCards.isEmpty() ? new String[]{"—"}
                : java.util.stream.IntStream.range(0, previewCards.size()).mapToObj(i -> (i + 1) + " / " + previewCards.size()).toArray(String[]::new));
        cardChoice.setCurrentIndex(0); singletonArrows(cardChoice, previewCards.size()); showCard();
    }
    private void showCard() {
        if (cardFrame == null) return;
        if (previewActor != null) Forge.retireAdventureResource(previewActor);
        previewActor = null; cardFrame.clearChildren();
        if (previewCards.isEmpty()) return;
        previewActor = forge.util.MyRandom.withRandom(new java.util.Random(), () ->
                new RewardActor(new Reward(previewCards.get(cardChoice.getCurrentIndex())), false, RewardScene.Type.Loot, false));
        previewActor.setName("deckCardPreview"); previewActor.setBounds(0, 0, cardFrame.getWidth(), cardFrame.getHeight()); cardFrame.addActor(previewActor);
    }
    private void closeDeckWindow() {
        if (previewActor != null) Forge.retireAdventureResource(previewActor);
        previewActor = null; cardFrame = null; cardList = null; cardChoice = null; deckWindow = null; editionField = null; editionCell = null;
    }

    private CharacterIdentity selectionIdentity() {
        CharacterIdentity identity = new CharacterIdentity();
        Gender selectedGender = catalog.genders.get(gender.getCurrentIndex()); Race selectedRace = catalog.races.get(race.getCurrentIndex());
        Appearance selectedAppearance = catalog.appearances.get(appearance.getCurrentIndex());
        identity.genderId = selectedGender.id; identity.genderLabel = label(selectedGender);
        identity.raceId = selectedRace.id; identity.raceLabel = label(selectedRace);
        identity.appearanceId = selectedAppearance.id; identity.appearanceSprite = selectedAppearance.sprite;
        identity.starterClassId = confirmedDeck.classId; identity.portrait = selectedPortrait; identity.portraitPng = importedPortrait.clone();
        return CharacterIdentity.fromSaveData(identity.toSaveData());
    }
    public boolean start() {
        if (started || startButton.isDisabled()) return true;
        started = true;
        if (selectedName.getText().trim().isEmpty()) selectedName.setText(randomName(catalog.genders.get(gender.getCurrentIndex())));
        TextField runName = Controls.newTextField(selectedName.getText()); runName.setMaxLength(80); runName.setName("playthroughName");
        Dialog prompt = createGenericDialog(text("lblNewPlaythrough", "New playthrough"), null,
                text("lblStart", "Start"), text("lblCancel", "Cancel"), () -> {
                    String name = runName.getText().trim(); removeDialog(); createPlaythrough(name.isEmpty() ? selectedName.getText() : name);
                }, () -> { started = false; removeDialog(); });
        prompt.getContentTable().add(Controls.newLabel(campaign.getTitle())).width(220).pad(5).row();
        prompt.getContentTable().add(Controls.newLabel(text("lblPlaythroughName", "Playthrough name"))).pad(5).row();
        prompt.getContentTable().add(runName).width(220).pad(5); showDialog(prompt); enableDialogNavigation(); stage.setKeyboardFocus(runName); return true;
    }
    private void createPlaythrough(String runName) {
        final String name = selectedName.getText().trim();
        final CharacterIdentity identity = selectionIdentity();
        final Appearance fallback = catalog.appearances.get(appearance.getCurrentIndex());
        final DifficultyData selectedDifficulty = selectedDifficulty(); final DeckState deck = confirmedDeck;
        if (!WorldSave.checkpointAndSuspend()) {
            started = false; showDialog(createGenericDialog(text("lblError", "Error"), WorldSave.getLastError(), text("lblOK", "OK"),
                    null, this::removeDialog, null, false, "", true)); return;
        }
        Runnable generate = () -> {
            try {
                CampaignManager.activate(campaign); WorldSave.beginPlaythrough(campaign, runName, name);
                WorldSave.generateNewWorld(name, !fallback.female, fallback.heroIndex, 0, deck.color, selectedDifficulty,
                        deck.mode, deck.deckIndex, deck.edition, 0, identity, new Deck(deck.deck));
                GamePlayerUtil.getGuiPlayer().setName(name); CampaignManager.applyStartingQuests();
                if (!WorldSave.saveInitialSnapshot()) {
                    Forge.clearTransitionScreen(); StartScene menu = StartScene.instance(); Forge.switchScene(menu);
                    menu.showError(text("lblInitialSaveFailed", "Your new playthrough is active, but its first save failed. Resume or try saving again.")
                            + "\n" + WorldSave.getLastError()); return;
                }
                SoundSystem.instance.changeBackgroundTrack(); WorldStage.getInstance().enterSpawnPOI(); Forge.switchScene(GameScene.instance());
            } catch (Exception error) {
                String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
                WorldSave.discardFailedPlaythrough(); if (WorldSave.canContinue()) WorldSave.loadContinue();
                Forge.clearTransitionScreen(); StartScene menu = StartScene.instance(); Forge.switchScene(menu); menu.showError(message);
            } finally { started = false; }
        };
        Forge.setTransitionScreen(new TransitionScreen(generate, null, false, true, text("lblGeneratingWorld", "Generating world")));
    }
    public boolean back() { Forge.switchScene(CampaignSelectionScene.instance()); return true; }
    @Override public boolean keyPressed(int keycode) {
        if (KeyBinding.Back.isPressed(keycode)) {
            if (portraitDialog != null && !dialogs.isEmpty() && dialogs.peek() == portraitDialog) closePortraitDialog();
            else if (deckWindow != null && !dialogs.isEmpty() && dialogs.peek() == deckWindow) { restoreDeck(confirmedDeck); removeDialog(); closeDeckWindow(); updateMainDeck(); }
            else if (!dialogs.isEmpty()) { started = false; removeDialog(); }
            else back();
            return true;
        }
        if (portraitDialog != null && stage.getKeyboardFocus() == portraitDialog.files) {
            // Stage also dispatches to List: its native arrows must move only once.
            if (KeyBinding.Up.isPressed(keycode)) {
                if (keycode != com.badlogic.gdx.Input.Keys.UP) portraitDialog.moveSelection(-1);
                return true;
            }
            if (KeyBinding.Down.isPressed(keycode)) {
                if (keycode != com.badlogic.gdx.Input.Keys.DOWN) portraitDialog.moveSelection(1);
                return true;
            }
            if (KeyBinding.Use.isPressed(keycode)) { portraitDialog.openSelection(); return true; }
        }
        return super.keyPressed(keycode);
    }
    @Override public boolean keyReleased(int keycode) {
        if (portraitDialog != null && stage.getKeyboardFocus() == portraitDialog.files
                && (KeyBinding.Up.isPressed(keycode) || KeyBinding.Down.isPressed(keycode) || KeyBinding.Use.isPressed(keycode))) return true;
        return super.keyReleased(keycode);
    }
    @Override public void enter() { unselectActors(); super.enter(); }
    @Override public void dispose() { disposed = true; retirePortrait(); closeDeckWindow(); super.dispose(); }
}
