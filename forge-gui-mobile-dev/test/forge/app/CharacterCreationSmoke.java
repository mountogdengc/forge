package forge.app;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Timer;
import forge.Forge;
import forge.adventure.campaign.CampaignRegistry;
import forge.adventure.campaign.CampaignManager;
import forge.adventure.character.CharacterIdentity;
import forge.adventure.character.StarterDeckPreview;
import forge.adventure.data.DifficultyData;
import forge.adventure.scene.GameScene;
import forge.adventure.scene.TileMapScene;
import forge.adventure.scene.NewGameScene;
import forge.adventure.scene.StartScene;
import forge.adventure.util.Selector;
import forge.adventure.util.Current;
import forge.adventure.util.Config;
import forge.adventure.util.AdventureModes;
import forge.adventure.util.AdventureOverrides;
import forge.card.ColorSet;
import forge.util.MyRandom;
import forge.adventure.world.WorldSave;
import forge.deck.io.DeckSerializer;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.HexFormat;
import java.security.MessageDigest;

/** Native rendering checks use only an invisible, unfocused window and an explicitly disposable profile. */
public final class CharacterCreationSmoke {
    private final Path output = Path.of(System.getProperty("creation.output"));
    private final long started = System.nanoTime();
    private int phase, settled;
    private long frame = -1;
    private NewGameScene creator;
    private final Properties expected = new Properties();
    private float cardWidth, cardHeight;
    private forge.adventure.data.CharacterCreationData.StarterClass invalidClass;
    private String originalClassDeck;
    private final java.util.List<String> regressionFailures=new java.util.ArrayList<>();

    public static void main(String[] args) {
        require(Boolean.getBoolean("creation.isolatedProfile"), "Disposable profile required");
        CharacterCreationSmoke probe = new CharacterCreationSmoke();
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Character creation validation");
        config.setWindowedMode(Boolean.getBoolean("creation.portrait") ? 1080 : 1920,
                Boolean.getBoolean("creation.portrait") ? 1920 : 1080);
        config.setInitialVisible(false); config.disableAudio(true);
        config.setForegroundFPS(30); config.setIdleFPS(15);
        config.setPauseWhenLostFocus(false); config.setPauseWhenMinimized(false);
        config.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override public void created(Lwjgl3Window window) {
                Timer.schedule(new Timer.Task() { @Override public void run() { probe.tick(); } }, 1f, .25f);
            }
        });
        String assets = Path.of(System.getProperty("creation.source"), "forge-gui").toString().replace('\\', '/') + "/";
        ApplicationListener app = Forge.getApp(null, new Lwjgl3Clipboard(), new Main.DesktopAdapter(probe.output.resolve("orientation.ini").toString()) {
            @Override public String getVersionString() { return "2.0.16-GITCharacterCreation"; }
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
            require((System.nanoTime() - started) / 1_000_000_000 < 300, "Creator timeout at " + phase);
            long handle = ((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle();
            require(GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_VISIBLE) == GLFW.GLFW_FALSE, "Window became visible");
            require(GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_FOCUSED) == GLFW.GLFW_FALSE, "Window acquired focus");
            Gdx.graphics.requestRendering();
            if (Forge.getTransitionScreen() != null) return;
            if (frame == Gdx.graphics.getFrameId()) return;
            frame = Gdx.graphics.getFrameId();
            if (phase == 0) {
                if (!(Forge.getCurrentScene() instanceof StartScene)) {
                    if (Forge.afterDBloaded && Forge.getSplashScreen() != null && Forge.getSplashScreen().isShowModeSelector()) {
                        Forge.clearSplashScreen(); Forge.switchToAdventure();
                    }
                    return;
                }
                Forge.clearSplashScreen(); Forge.clearCurrentScreen(); Forge.startContinuousRendering();
                if (Boolean.getBoolean("creation.resume")) {
                    try (var input = Files.newInputStream(output.resolve("expected.properties"))) { expected.load(input); }
                    require(WorldSave.canContinue(), "New identity save is unavailable in a fresh process");
                    StartScene.instance().Continue(); phase = 8; return;
                }
                long openCalls=MyRandom.callCount(); byte[] openRandom=randomState(Current.world().getRandom());
                creator = NewGameScene.forCampaign(CampaignRegistry.discover().findByPlane("CurseOfStrahd"));
                require(MyRandom.callCount()==openCalls && java.util.Arrays.equals(openRandom,randomState(Current.world().getRandom())),
                        "Opening the creator changed the outgoing random state");
                Forge.switchScene(creator); phase = 1; return;
            }
            if (phase == 1) {
                if (++settled < 3) return;
                require(Forge.getCurrentScene() == creator, "Creator is not the active rendered scene");
                require(actor("campaignTitle") != null, "Campaign title is missing from the creator");
                require(actor("importPortrait") != null, "Portrait import control is missing");
                require(actor("appearance") instanceof Selector, "Independent arrow appearance selector is missing");
                capture("creator");
                for (String name : new String[]{"gender", "race", "portraits", "appearance", "difficulty"}) {
                    Selector selector = (Selector) actor(name);
                    require(selector.getLabel().getHeight() <= selector.getHeight() + .01f, "Theme button exceeds selector: " + name);
                }
                require(!((CheckBox)actor("matchPortraitRace")).isChecked() && !((CheckBox)actor("matchPortraitGender")).isChecked(), "Portrait filters should default off");
                Selector gender = (Selector)actor("gender"), race = (Selector)actor("race"), appearance = (Selector)actor("appearance");
                creator.getUI().getStage().setKeyboardFocus(gender);
                require(creator.getSelected()!=null && creator.getSelected().actor==gender,
                        "Keyboard navigation registered selector children instead of the selector");
                int previousGender=gender.getCurrentIndex();
                creator.getUI().getStage().keyDown(Input.Keys.RIGHT); creator.getUI().getStage().keyUp(Input.Keys.RIGHT);
                require(creator.getUI().getStage().getKeyboardFocus()==gender
                        && gender.getCurrentIndex()==(previousGender+1)%gender.getTextList().size,
                        "Right key must cycle the focused selector without moving focus");
                TextureRegion before = portrait();
                gender.setCurrentIndex(2); race.setCurrentIndex(race.getCurrentIndex()+1); appearance.setCurrentIndex(appearance.getCurrentIndex()+1);
                require(portrait().getTexture() == before.getTexture(), "Independent gender/race/sprite changed the portrait");
                require(!appearance.getLabel().isVisible(), "Appearance name must be hidden");
                var appearanceCatalogField=NewGameScene.class.getDeclaredField("catalog"); appearanceCatalogField.setAccessible(true);
                var appearanceCatalog=(forge.adventure.character.CharacterCreationCatalog)appearanceCatalogField.get(creator);
                var selectedAppearance=appearanceCatalog.appearances.get(appearance.getCurrentIndex());
                String originalSprite=selectedAppearance.sprite;
                try {
                    selectedAppearance.sprite="sprites/missing-creator-test.atlas"; invoke("updateAppearance");
                    require(((Button)actor("start")).isDisabled() && ((Image)actor("appearancePreview")).getDrawable()==null,
                            "Missing campaign sprite must disable Begin without crashing");
                } finally { selectedAppearance.sprite=originalSprite; invoke("updateAppearance"); }
                require(!((Button)actor("start")).isDisabled() && portrait().getTexture()==before.getTexture(),
                        "Valid appearance did not recover independently");
                gender.setCurrentIndex(1);
                ((CheckBox)actor("matchPortraitGender")).setChecked(true);
                ((CheckBox)actor("matchPortraitRace")).setChecked(true);
                require(((Button)actor("start")).isDisabled() && ((Image)actor("avatarPreview")).getDrawable() == null, "Empty portrait intersection must be explicit");
                Path image = output.resolve("portrait-source.png");
                var jpeg=new java.awt.image.BufferedImage(800,600,java.awt.image.BufferedImage.TYPE_INT_RGB);
                var brush=jpeg.createGraphics(); try { brush.setColor(java.awt.Color.BLUE); brush.fillRect(0,0,800,600); } finally { brush.dispose(); }
                Path pictures=Path.of(System.getProperty("user.home"),"Pictures"); Files.createDirectories(pictures);
                Path jpegPath=pictures.resolve("portrait-source.jpg"); javax.imageio.ImageIO.write(jpeg,"jpg",jpegPath.toFile());
                Files.copy(jpegPath,pictures.resolve("0-other.jpg"),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                Files.copy(jpegPath,pictures.resolve("z-other.jpg"),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                invoke("choosePortraitFile");
                require(actor("portraitPath") instanceof com.badlogic.gdx.scenes.scene2d.ui.TextField,"Adventure import picker is missing");
                var importPath=(com.badlogic.gdx.scenes.scene2d.ui.TextField)actor("portraitPath");
                Actor fileList=actor("portraitFiles"); creator.getUI().getStage().setKeyboardFocus(fileList);
                @SuppressWarnings("unchecked") var pickerFiles=(com.badlogic.gdx.scenes.scene2d.ui.List<String>)fileList;
                pickerFiles.setSelectedIndex(0);
                creator.getUI().getStage().keyDown(Input.Keys.DOWN); creator.getUI().getStage().keyUp(Input.Keys.DOWN);
                require(pickerFiles.getSelectedIndex()==1,"Picker Down must move exactly one entry");
                creator.getUI().getStage().keyDown(Input.Keys.UP); creator.getUI().getStage().keyUp(Input.Keys.UP);
                require(pickerFiles.getSelectedIndex()==0,"Picker Up must move exactly one entry");
                creator.getUI().getStage().keyDown(Input.Keys.DPAD_DOWN); creator.getUI().getStage().keyUp(Input.Keys.DPAD_DOWN);
                require(pickerFiles.getSelectedIndex()==1,"Picker DPAD Down must move exactly one entry");
                require(creator.getUI().getStage().getKeyboardFocus()==fileList,"Picker directional keys moved focus away from file selection");
                creator.getUI().getStage().keyDown(Input.Keys.BUTTON_A); creator.getUI().getStage().keyUp(Input.Keys.BUTTON_A);
                require(actor("portraitPath")==null,"Successful import did not close the picker");
                require(portrait().getRegionWidth()==512 && portrait().getRegionHeight()==512,"JPEG import failed");
                Pixmap pixels = new Pixmap(640, 800, Pixmap.Format.RGBA8888);
                try {
                    pixels.setColor(.2f,.4f,.7f,1); pixels.fill();
                    pixels.setColor(1,0,0,1); pixels.fillRectangle(0,0,640,80);
                    pixels.setColor(0,1,0,1); pixels.fillRectangle(0,720,640,80);
                    PixmapIO.writePNG(Gdx.files.absolute(image.toString()), pixels);
                }
                finally { pixels.dispose(); }
                creator.importPortrait(image);
                require(!((Button)actor("start")).isDisabled(), "Imported portrait did not recover an empty filter");
                require(portrait().getRegionWidth()==512 && portrait().getRegionHeight()==512, "Imported portrait was not normalized");
                before = portrait(); gender.setCurrentIndex(3);
                require(portrait().getTexture() == before.getTexture(), "Filter change replaced imported portrait");
                Path invalid = output.resolve("not-an-image.txt"); Files.writeString(invalid,"not an image");
                invoke("choosePortraitFile"); importPath=(com.badlogic.gdx.scenes.scene2d.ui.TextField)actor("portraitPath"); importPath.setText(invalid.toString());
                creator.getUI().getStage().setKeyboardFocus(importPath);
                creator.getUI().getStage().keyDown(Input.Keys.ENTER); creator.getUI().getStage().keyUp(Input.Keys.ENTER);
                require(actor("portraitPath")!=null && !((Label)actor("portraitImportError")).getText().isEmpty()
                        && portrait().getTexture()==before.getTexture(), "Invalid import destroyed a valid portrait");
                creator.getUI().getStage().keyDown(Input.Keys.ESCAPE); creator.getUI().getStage().keyUp(Input.Keys.ESCAPE);
                CharacterIdentity identity = identity();
                Pixmap normalized=new Pixmap(identity.portraitPng,0,identity.portraitPng.length);
                try { require(normalized.getPixel(256,16)==normalized.getPixel(256,256)
                        && normalized.getPixel(256,496)==normalized.getPixel(256,256),"Portrait was stretched rather than center-cropped"); }
                finally { normalized.dispose(); }
                expected.setProperty("gender", identity.genderId); expected.setProperty("race", identity.raceId);
                expected.setProperty("sprite", identity.appearanceSprite); expected.setProperty("png", hash(identity.portraitPng));
                invoke("showDeckWindow"); phase=2; settled=0; return;
            }
            if (phase==2) {
                if (++settled<3) return;
                Actor selectorFocus=creator.getUI().getStage().getKeyboardFocus();
                creator.getUI().getStage().keyDown(Input.Keys.DOWN); creator.getUI().getStage().keyUp(Input.Keys.DOWN);
                require(creator.getUI().getStage().getKeyboardFocus()!=selectorFocus,
                        "Deck window must allow directional navigation to cards and confirmation");
                Actor frame=actor("deckCardFrame"), card=actor("deckCardPreview");
                require(frame!=null && card!=null, "Actual card renderer is missing");
                cardWidth=frame.getWidth(); cardHeight=frame.getHeight();
                require(Math.abs(cardWidth/cardHeight-488f/680f)<.005f, "Card frame has an incorrect aspect ratio");
                Selector cards=(Selector)actor("cardChoice"); cards.setCurrentIndex(cards.getTextList().size-1);
                require(actor("deckCardFrame").getWidth()==cardWidth && actor("deckCardFrame").getHeight()==cardHeight, "Card text changed preview bounds");
                var cardsField=NewGameScene.class.getDeclaredField("previewCards"); cardsField.setAccessible(true);
                @SuppressWarnings("unchecked") java.util.List<forge.item.PaperCard> foilCards=(java.util.List<forge.item.PaperCard>)cardsField.get(creator);
                var foil=foilCards.get(0).getFoiled(); foilCards.clear(); foilCards.add(foil);
                long foilCalls=MyRandom.callCount(); cards.setTextList(new String[]{"1 / 1"});
                if(MyRandom.callCount()!=foilCalls)regressionFailures.add("Foil preview advanced game RNG/counter");
                ((Selector)actor("starterDeck")).setCurrentIndex(1);
                creator.getUI().getStage().keyDown(Input.Keys.ESCAPE); creator.getUI().getStage().keyUp(Input.Keys.ESCAPE);
                require(actor("confirmStarter")==null && ((Label)actor("starterSummary")).getText().toString().contains("Paladin"),
                        "Back must cancel the unconfirmed class and restore the main selection");
                invoke("showDeckWindow");
                ((Selector)actor("starterDeck")).setCurrentIndex(1); phase=3; settled=0; return;
            }
            if (phase==3) {
                if (++settled<3) return;
                require(actor("deckCardFrame").getWidth()==cardWidth && actor("deckCardFrame").getHeight()==cardHeight, "Deck change resized card frame");
                var catalogField=NewGameScene.class.getDeclaredField("catalog"); catalogField.setAccessible(true);
                var catalog=(forge.adventure.character.CharacterCreationCatalog)catalogField.get(creator);
                invalidClass=catalog.starterClasses.get(1); originalClassDeck=invalidClass.deck;
                invalidClass.deck="decks/missing-native-deck.dck";
                ((Selector)actor("starterDeck")).setCurrentIndex(0); ((Selector)actor("starterDeck")).setCurrentIndex(1);
                require(((Button)actor("confirmStarter")).isDisabled(),"Unavailable starter must disable confirmation");
                creator.performTouch(actor("confirmStarter")); phase=30; settled=0; return;
            }
            if (phase==30) {
                if (++settled<3) return;
                require(actor("confirmStarter")!=null,"Invalid confirmation left a hidden, unreopenable deck window");
                invalidClass.deck=originalClassDeck;
                ((Selector)actor("starterDeck")).setCurrentIndex(0); ((Selector)actor("starterDeck")).setCurrentIndex(1);
                require(!((Button)actor("confirmStarter")).isDisabled(),"Valid deck did not recover confirmation");
                phase=31; settled=0; return;
            }
            if (phase==31) {
                if (++settled<3) return;
                capture("class-deck"); creator.performTouch(actor("confirmStarter")); phase=4; settled=0; return;
            }
            if (phase==4) {
                if (++settled<3) return;
                require(((Label)actor("starterSummary")).getText().toString().contains("Necromancer"), "Confirmed class missing from main screen");
                require(!((Label)actor("starterSummary")).getText().toString().contains("Grave Resolve"), "Redundant deck title is visible");
                expected.setProperty("class", identity().starterClassId);
                creator.start();
                Actor runName=actor("playthroughName"); require(runName!=null,"Playthrough name prompt missing");
                creator.getUI().getStage().keyDown(Input.Keys.DOWN); creator.getUI().getStage().keyUp(Input.Keys.DOWN);
                require(creator.getUI().getStage().getKeyboardFocus()!=runName,"Name prompt blocks navigation to confirmation");
                creator.getUI().getStage().keyDown(Input.Keys.ESCAPE); creator.getUI().getStage().keyUp(Input.Keys.ESCAPE);
                require(actor("playthroughName")==null,"Back did not cancel the name prompt");
                try { verifyGeneratedPreview(); } catch(AssertionError error) { regressionFailures.add(error.getMessage()); }
                try { verifyDifficultyRetention(); } catch(AssertionError error) { regressionFailures.add(error.getMessage()); }
                require(regressionFailures.isEmpty(),String.join("; ",regressionFailures));
                if (Boolean.getBoolean("creation.uiOnly")) { verifyInherited(); phase=9; settled=0; return; }
                invoke("createPlaythrough", "Character creation native test"); phase=5; return;
            }
            if (phase==5) {
                if (!explorationScene()) return;
                verifySavedIdentity();
                String deck=Current.player().getSelectedDeck().getMain().toCardList("\n");
                var original=DeckSerializer.fromFile(Path.of(System.getProperty("creation.source"),"forge-gui/res/adventure/CurseOfStrahd/decks/barovia-black.dck").toFile());
                require(deck.equals(original.getMain().toCardList("\n")), "Character creation changed the campaign starter card list");
                expected.setProperty("deck",deck);
                expected.setProperty("life",Integer.toString(Current.player().getLife())); expected.setProperty("gold",Integer.toString(Current.player().getGold()));
                // The normal opening can have a pending quest dialogue. Its initial snapshot already contains
                // the complete identity; do not clear or skip gameplay just to make a manual save possible.
                if (WorldSave.explorationSaveBlockReason().isEmpty())
                    require(WorldSave.saveManual("Imported portrait validation"), "New identity save failed: "+WorldSave.getLastError());
                else System.out.println("CREATION_INITIAL_SAVE_USED: " + WorldSave.explorationSaveBlockReason());
                try(var out=Files.newOutputStream(output.resolve("expected.properties"))){expected.store(out,"Character creation native expectations");}
                Files.delete(output.resolve("portrait-source.png"));
                Files.delete(Path.of(System.getProperty("user.home"),"Pictures/portrait-source.jpg"));
                pass("CHARACTER_CREATION_NEW_SAVE_PASS"); return;
            }
            if (phase==6 || phase==8) {
                if (!explorationScene()) return;
                verifySavedIdentity();
                require(Current.player().getSelectedDeck().getMain().toCardList("\n").equals(expected.getProperty("deck")), "Continue changed starter cards");
                require(Current.player().getLife()==Integer.parseInt(expected.getProperty("life")) && Current.player().getGold()==Integer.parseInt(expected.getProperty("gold")), "Continue changed life or currency");
                require(!Files.exists(output.resolve("portrait-source.png")), "Portrait portability needs the source file removed");
                capture(phase==8?"fresh-continue":"continued");
                if (phase==8) { pass("CHARACTER_CREATION_FRESH_CONTINUE_PASS"); return; }
                verifyInherited(); phase=9; settled=0; return;
            }
            if (phase==9) {
                if (++settled<3) return;
                capture("inherited-campaign"); pass("CHARACTER_CREATION_NATIVE_PASS");
            }
        } catch (Throwable problem) { problem.printStackTrace(); System.exit(1); }
    }

    private Actor actor(String name) { return creator.getUI().getStage().getRoot().findActor(name); }
    private boolean explorationReady() {
        return explorationScene() && WorldSave.explorationSaveBlockReason().isEmpty();
    }
    private boolean explorationScene() { return Forge.getCurrentScene() instanceof GameScene || Forge.getCurrentScene() instanceof TileMapScene; }
    private TextureRegion portrait() { return ((TextureRegionDrawable)((Image)actor("avatarPreview")).getDrawable()).getRegion(); }
    private Object invoke(String name, Object... arguments) throws Exception {
        Class<?>[] types=java.util.Arrays.stream(arguments).map(Object::getClass).toArray(Class<?>[]::new);
        var method=NewGameScene.class.getDeclaredMethod(name,types); method.setAccessible(true); return method.invoke(creator,arguments);
    }
    private CharacterIdentity identity() throws Exception { return (CharacterIdentity)invoke("selectionIdentity"); }
    private static String hash(byte[] value) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)); }
    private void verifySavedIdentity() throws Exception {
        CharacterIdentity value=Current.player().getCharacterIdentity(); require(value!=null,"New identity section was lost");
        require(value.genderId.equals(expected.getProperty("gender")) && value.raceId.equals(expected.getProperty("race")),"Independent gender/race did not persist");
        require(value.appearanceSprite.equals(expected.getProperty("sprite")) && value.starterClassId.equals(expected.getProperty("class")),"Appearance or class did not persist");
        require(hash(value.portraitPng).equals(expected.getProperty("png")),"Imported portrait bytes did not persist");
        require(Current.player().avatar().getRegionWidth()==512 && Current.player().avatar().getRegionHeight()==512,"Saved imported portrait did not render");
        require(Current.player().spriteName().equals(value.appearanceSprite),"Saved independent appearance was not used");
    }
    private void verifyInherited() {
        String active=CampaignManager.current().getId();
        creator=NewGameScene.forCampaign(CampaignRegistry.discover().findByPlane("Bellwake")); Forge.switchScene(creator);
        require(CampaignManager.current().getId().equals(active),"Preview activated another campaign");
        require(((Label)actor("starterSummary")).getText().toString().contains("Lantern Watch"),"Inherited campaign lost its starter choices");
    }
    private void verifyGeneratedPreview() throws Exception {
        Config active=Config.instance();
        Config preview=Config.forCampaign(CampaignRegistry.discover().findByPlane("Bellwake"));
        java.util.Random random=Current.world().getRandom(); byte[] before=randomState(random);
        AdventureOverrides overrides=AdventureOverrides.instance(); long calls=MyRandom.callCount();
        DifficultyData difficulty=new DifficultyData(); difficulty.pileDecks=new com.badlogic.gdx.utils.ObjectMap<>();
        difficulty.pileDecks.put("W","decks/starter/pile_white_e.json");
        var playerMode=forge.adventure.player.AdventurePlayer.class.getDeclaredField("adventureMode"); playerMode.setAccessible(true);
        Object originalMode=playerMode.get(Current.player()); playerMode.set(Current.player(),AdventureModes.Commander);
        var selectedColor=ColorSet.fromNames(Current.player().getColorIdentity().getColor()==forge.card.MagicColor.WHITE ? "B".toCharArray() : "W".toCharArray());
        forge.deck.Deck deck;
        try { deck=StarterDeckPreview.generate(preview,difficulty,AdventureModes.Pile,selectedColor,()->{
            require(Config.instance()==preview,"Generated preview uses the outgoing campaign policy");
            require(Current.world().getRandom()!=random,"Generated preview consumes the outgoing world's RNG");
            require(AdventureOverrides.instance()!=overrides,"Generated preview uses the outgoing booster overrides");
            for (var previewCard : forge.adventure.data.RewardData.getAllCards()) {
                require(!previewCard.getRules().getAiHints().getRemNonCommanderDecks(),
                        "Generated preview inherited outgoing Commander policy");
            }
            var rule=new forge.adventure.data.RewardData(); rule.colors=new String[]{"colorID"};
            var predicate=new forge.adventure.util.CardUtil.CardPredicate(rule,true);
            var white=forge.StaticData.instance().getCommonCards().getCard("Doomed Traveler","ISD");
            var black=forge.StaticData.instance().getCommonCards().getCard("Diregraf Ghoul","ISD");
            require(predicate.test(white)==(selectedColor.getColor()==forge.card.MagicColor.WHITE)
                    && predicate.test(black)==(selectedColor.getColor()==forge.card.MagicColor.BLACK),"Generated preview inherited outgoing color identity");
            return preview.starterDeck(ColorSet.fromNames("W".toCharArray()),difficulty,AdventureModes.Pile,0,null);
        }); } finally { playerMode.set(Current.player(),originalMode); }
        require(deck!=null && !deck.getMain().isEmpty(),"Inherited JSON starter preview generated no cards");
        require(Config.instance()==active && AdventureOverrides.instance()==overrides,"Preview context leaked after success");
        require(Current.world().getRandom()==random && java.util.Arrays.equals(before,randomState(random)),"Preview altered outgoing world RNG state");
        require(MyRandom.callCount()==calls,"Preview advanced the game's random counter");
        try { StarterDeckPreview.generate(preview,difficulty,AdventureModes.Pile,selectedColor,()->{throw new IllegalStateException("probe");}); }
        catch (IllegalStateException expected) { require(expected.getMessage().equals("probe"),"Wrong context exception"); }
        require(Config.instance()==active && Current.world().getRandom()==random && AdventureOverrides.instance()==overrides,"Preview context leaked after failure");
    }
    private static byte[] randomState(java.util.Random random) throws Exception {
        var bytes=new java.io.ByteArrayOutputStream(); try(var out=new java.io.ObjectOutputStream(bytes)){out.writeObject(random);} return bytes.toByteArray();
    }
    private void verifyDifficultyRetention() throws Exception {
        var definition=CampaignRegistry.discover().findByPlane("Bellwake");
        Config resources=Config.forCampaign(definition);
        resources.getConfigData().allowedStarterModes=null;
        DifficultyData first=resources.getConfigData().difficulties[0];
        first.starterDecks=first.constructedStarterDecks; first.startingDifficulty=true;
        var json=new com.badlogic.gdx.utils.Json();
        DifficultyData second=json.fromJson(DifficultyData.class,json.toJson(first));
        second.name="Alternative"; second.startingDifficulty=false;
        resources.getConfigData().difficulties=new DifficultyData[]{first,second};
        resources.getConfigData().starterDecksByEdition=new com.badlogic.gdx.utils.ObjectMap<>();
        var partialEdition=new com.badlogic.gdx.utils.ObjectMap<String,String>(); partialEdition.put("B","decks/quay-white.dck");
        resources.getConfigData().starterDecksByEdition.put(resources.starterEditions()[0],partialEdition);
        var constructor=NewGameScene.class.getDeclaredConstructor(forge.adventure.campaign.CampaignDefinition.class,Config.class);
        constructor.setAccessible(true); NewGameScene legacy=constructor.newInstance(definition,resources);
        try {
            var modeField=NewGameScene.class.getDeclaredField("mode"); modeField.setAccessible(true);
            Selector mode=(Selector)modeField.get(legacy); int standard=0;
            for(int i=0;i<mode.getTextList().size;i++)if(mode.getTextList().get(i).contains("Standard"))standard=i;
            mode.setCurrentIndex(standard);
            var working=NewGameScene.class.getDeclaredField("workingDeck"); working.setAccessible(true);
            var confirmed=NewGameScene.class.getDeclaredField("confirmedDeck"); confirmed.setAccessible(true);
            Object chosen=working.get(legacy); confirmed.set(legacy,chosen);
            Selector difficulty=legacy.getUI().findActor("difficulty"); difficulty.setCurrentIndex(1);
            require(mode.getCurrentIndex()==standard,"Difficulty change silently replaced the confirmed starter mode");
            var cards=chosen.getClass().getDeclaredField("deck"); cards.setAccessible(true);
            require(cards.get(confirmed.get(legacy))==cards.get(chosen),"Unchanged starter source should retain the confirmed deck rather than reroll");
            second.starterDecks.put("W","decks/quay-red.dck");
            difficulty.setCurrentIndex(0); difficulty.setCurrentIndex(1);
            require(mode.getCurrentIndex()==standard && confirmed.get(legacy)==null,
                    "Changed starter contents must retain the selected mode but require confirmation");
            require(((Button)legacy.getUI().findActor("start")).isDisabled(),"Changed starter contents were silently confirmed");
            first.constructedStarterDecks.remove("R"); second.constructedStarterDecks.remove("R");
            first.starterDecks.put("R","decks/quay-white.dck"); second.starterDecks.put("R","decks/quay-red.dck");
            difficulty.setCurrentIndex(0);
            int constructed=0; for(int i=0;i<mode.getTextList().size;i++)if(mode.getTextList().get(i).contains("Constructed"))constructed=i;
            mode.setCurrentIndex(constructed);
            var choiceField=NewGameScene.class.getDeclaredField("deckChoice"); choiceField.setAccessible(true);
            Selector choice=(Selector)choiceField.get(legacy); choice.setCurrentIndex(1); confirmed.set(legacy,working.get(legacy));
            difficulty.setCurrentIndex(1);
            require(mode.getCurrentIndex()==constructed && confirmed.get(legacy)==null,"Constructed-to-Standard fallback changed without reconfirmation");
            first.pileDecks=new com.badlogic.gdx.utils.ObjectMap<>(); second.pileDecks=new com.badlogic.gdx.utils.ObjectMap<>();
            first.pileDecks.put("W","decks/quay-white.dck"); second.pileDecks.put("W","decks/quay-white.dck");
            first.commanderDecks=new com.badlogic.gdx.utils.ObjectMap<>(); second.commanderDecks=new com.badlogic.gdx.utils.ObjectMap<>();
            first.commanderDecks.put("R","decks/quay-white.dck"); second.commanderDecks.put("R","decks/quay-red.dck");
            difficulty.setCurrentIndex(0);
            int pile=0; for(int i=0;i<mode.getTextList().size;i++)if(mode.getTextList().get(i).contains("Pile"))pile=i;
            mode.setCurrentIndex(pile); choice.setCurrentIndex(1); confirmed.set(legacy,working.get(legacy)); difficulty.setCurrentIndex(1);
            require(mode.getCurrentIndex()==pile && confirmed.get(legacy)==null,"Pile-to-Commander fallback changed without reconfirmation");
            var source=NewGameScene.class.getDeclaredMethod("deckSource",AdventureModes.class,ColorSet.class,int.class); source.setAccessible(true);
            long sourceCalls=MyRandom.callCount();
            Object one=source.invoke(legacy,AdventureModes.Precon,ColorSet.fromNames("W".toCharArray()),0);
            Object two=source.invoke(legacy,AdventureModes.Precon,ColorSet.fromNames("W".toCharArray()),0);
            require(one.equals(two) && sourceCalls==MyRandom.callCount(),"Random precon source key rerolled its candidate");
        } finally { legacy.dispose(); }
    }
    private void pass(String message) throws Exception { Files.writeString(output.resolve("PASS"),message+"\n"); System.out.println(message); System.exit(0); }
    private void capture(String name) {
        Pixmap shot = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        try { PixmapIO.writePNG(Gdx.files.absolute(output.resolve(name + ".png").toString()), shot, 6, true); }
        finally { shot.dispose(); }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
