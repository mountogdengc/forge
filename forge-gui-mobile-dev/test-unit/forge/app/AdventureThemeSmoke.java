package forge.app;

import com.badlogic.gdx.*;
import com.badlogic.gdx.backends.lwjgl3.*;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import forge.Forge;
import forge.adventure.campaign.*;
import forge.adventure.scene.*;
import forge.adventure.stage.MapStage;
import forge.adventure.theme.*;
import forge.adventure.util.*;
import forge.adventure.world.WorldSave;
import forge.assets.FSkinColor;
import forge.card.ColorSet;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Real native UI probe. No desktop input, network, normal profile, or gameplay win claims. */
public final class AdventureThemeSmoke {
    private int phase,frames;
    private int readyMatchFrames;
    private long lastFrame=-1,started=System.nanoTime();
    private String originalSkin,classicColor,createdId;
    private Set<String> beforeThemes;
    private final Path output=Path.of(System.getProperty("theme.output"));
    private final String expected=System.getProperty("theme.expected","folio");

    public static void main(String[] args){
        require(Boolean.getBoolean("theme.isolatedProfile"),"A disposable profile is required");
        String source=System.getProperty("theme.source");require(source!=null,"Explicit source root required");
        AdventureThemeSmoke smoke=new AdventureThemeSmoke();
        Lwjgl3ApplicationConfiguration cfg=new Lwjgl3ApplicationConfiguration();
        int width=Integer.getInteger("theme.width",1280),height=Integer.getInteger("theme.height",720);
        cfg.setTitle("Forge Adventure theme validation");cfg.setWindowedMode(width,height);cfg.setInitialVisible(false);
        cfg.setForegroundFPS(30);cfg.setIdleFPS(15);cfg.setPauseWhenLostFocus(false);cfg.setPauseWhenMinimized(false);cfg.disableAudio(true);
        cfg.setWindowListener(new Lwjgl3WindowAdapter(){@Override public void created(Lwjgl3Window window){
            Timer.schedule(new Timer.Task(){@Override public void run(){smoke.tick();}},1,.3f);
        }});
        String assets=Path.of(source,"forge-gui").toString().replace('\\','/')+"/";
        ApplicationListener app=Forge.getApp(null,new Lwjgl3Clipboard(),new Main.DesktopAdapter(smoke.output.resolve("orientation.ini").toString()){
            @Override public String getVersionString(){return "2.0.16-GITThemeSmoke";}
            @Override public boolean isConnectedToInternet(){return false;}
            @Override public boolean isConnectedToWifi(){return false;}
            @Override public void setMouseConfined(boolean confined){ }
            @Override public void preventSystemSleep(boolean prevent){ }
        },assets,height>width,false,0);
        Forge.selector="Adventure";new Lwjgl3Application(app,cfg);
    }
    private void tick(){
        try{
            long frame=Gdx.graphics.getFrameId();if(frame==lastFrame)return;lastFrame=frame;
            long handle=((Lwjgl3Graphics)Gdx.graphics).getWindow().getWindowHandle();
            require(org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(handle,org.lwjgl.glfw.GLFW.GLFW_VISIBLE)==org.lwjgl.glfw.GLFW.GLFW_FALSE,"Probe became visible");
            require(org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(handle,org.lwjgl.glfw.GLFW.GLFW_FOCUSED)==org.lwjgl.glfw.GLFW.GLFW_FALSE,"Probe acquired desktop focus");
            if((System.nanoTime()-started)/1_000_000_000>600)throw new AssertionError("Theme probe timed out at "+phase);
            Gdx.graphics.requestRendering();
            if(phase==0){
                if(Forge.getCurrentScene() instanceof StartScene&&Forge.getTransitionScreen()==null){
                    Forge.clearSplashScreen();Forge.clearCurrentScreen();Forge.startContinuousRendering();phase=1;
                }else if(Forge.afterDBloaded&&Forge.getSplashScreen().isShowModeSelector()){Forge.clearSplashScreen();Forge.switchToAdventure();}
                return;
            }
            if(++frames<2||Forge.getTransitionScreen()!=null)return;frames=0;
            AdventureThemeRuntime runtime=AdventureThemeRuntime.current();
            System.out.println("THEME_STEP "+phase+" active="+runtime.activeId()+" pending="+runtime.pendingId());
            switch(phase){
                case 1:
                    require(Path.of(SettingsScene.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(
                            Path.of(System.getProperty("theme.source"),"forge-gui-mobile/target/classes").toRealPath()),"Native probe must use the active source class directories");
                    Controls.getSkin();require(expected.equals(runtime.activeId()),"Wrong active startup theme: "+runtime.activeId());
                    capture("01-start");
                    require(Path.of(AdventureThemeSmoke.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath().equals(output.resolve("classes").toRealPath()),"Native fixture must be freshly compiled in this evidence directory");
                    if(System.getProperty("theme.inputProbe","").equals("assets")){assetProbe(runtime);return;}
                    if(System.getProperty("theme.inputProbe","").equals("cache")){
                        java.lang.ref.WeakReference<Config> previous=temporaryCampaignConfig(runtime);
                        for(int i=0;i<12&&previous.get()!=null;i++){System.gc();Thread.sleep(50);}
                        require(previous.get()==null,"Theme runtime strongly retains a retired campaign configuration");
                        finish("THEME_CACHE_PASS");return;
                    }
                    if(Boolean.getBoolean("theme.bespoke")){compatibilityProbe(runtime);return;}
                    if(Boolean.getBoolean("theme.resume")){
                        require(runtime.resolveActive(Config.instance()).ornament().equals("quiet"),"Saved ornament did not survive restart");
                        require(runtime.resolveActive(Config.instance()).texture()==0,"Saved texture did not survive restart");
                        finish("THEME_RESTART_PASS");return;
                    }
                    originalSkin=FModel.getPreferences().getPref(FPref.UI_SKIN);classicColor=FSkinColor.get(FSkinColor.Colors.CLR_TEXT).getColor().toString();
                    require(!WorldSave.hasActivePlaythrough(),"Appearance fixture must begin without a playthrough");
                    Forge.switchScene(SettingsScene.instance());phase=2;break;
                case 2:
                    capture("02-settings");
                    Actor entry=find(stage().getRoot(),"open-theme-appearance");require(entry!=null,"Settings entry missing");click(entry);
                    phase=3;break;
                case 3:
                    require(Forge.getCurrentScene() instanceof AppearanceScene,"Settings entry did not open Appearance");
                    capture("03-appearance");bounds("theme-apply");bounds("theme-customize");
                    require(stage().getRoot().findActor("preview-title")!=null,"Native component preview missing");
                    beforeThemes=ids(runtime.discover());click(find(stage().getRoot(),"theme-customize"));phase=4;break;
                case 4:
                    if(inputProbe(runtime))return;
                    require(Forge.getCurrentScene() instanceof ThemeWorkshopScene,"Workshop missing");capture("04-workshop-components");
                    bounds("theme-save-as");bounds("theme-restore");
                    previewContained();verifySwatch("action.primary");
                    TextField accent=find(stage().getRoot(),"theme-color-action.primary"),ink=find(stage().getRoot(),"theme-color-action.onPrimary");
                    type(ink,accent.getText());require(((Button)find(stage().getRoot(),"theme-save-as")).isDisabled(),"Low contrast recipe allowed saving");
                    require(expected.equals(runtime.activeId()),"Invalid preview changed active theme");capture("05-invalid-pair");
                    click(find(stage().getRoot(),"theme-restore"));
                    type(find(stage().getRoot(),"theme-name"),"Smoke variant "+expected);
                    ((SelectBox<?>)find(stage().getRoot(),"theme-ornament")).setSelectedIndex(0);
                    ((Slider)find(stage().getRoot(),"theme-texture")).setValue(0);
                    require(!((Button)find(stage().getRoot(),"theme-save-as")).isDisabled(),"Valid variant blocked");
                    phase=5;break;
                case 5:
                    TextField nameField=find(stage().getRoot(),"theme-name");stage().setKeyboardFocus(nameField);
                    int before=nameField.getText().length();stage().keyTyped('Z');
                    require(nameField.getText().length()==before+1,"Native text input did not reach the focused name field");
                    type(nameField,"Smoke variant "+expected+" — A long translated title for the planeswalker's travelling collection of field notes");
                    TextButton saveButton=find(stage().getRoot(),"theme-save-as");String saveCaption=saveButton.getText().toString();
                    saveButton.setText("Als neues Erscheinungsbild speichern");capture("06-long-labels");bounds("theme-save-as");require(saveButton.getLabel().getPrefHeight()<=saveButton.getHeight()-2,"Translated save caption clips in its button");saveButton.setText(saveCaption);
                    capture("06-workshop-quiet");clickCaption("Adventure");phase=6;break;
                case 6:
                    capture("07-workshop-adventure");clickCaption("Duel");phase=7;break;
                case 7:
                    capture("08-workshop-duel");click(find(stage().getRoot(),"theme-save-as"));
                    Set<String> after=ids(runtime.discover());after.removeAll(beforeThemes);require(after.size()==1,"Save-as must create exactly one recipe");createdId=after.iterator().next();
                    require(expected.equals(runtime.pendingId())&&expected.equals(runtime.activeId()),"Save-as selected or applied the recipe");
                    require(!WorldSave.hasActivePlaythrough(),"Theme authoring created gameplay state");
                    clickCaption("Back");phase=8;break;
                case 8:
                    require(Forge.getCurrentScene() instanceof AppearanceScene,"Back did not restore Appearance");
                    selectId(find(stage().getRoot(),"theme-choice"),createdId);click(find(stage().getRoot(),"theme-apply"));
                    require(createdId.equals(runtime.pendingId())&&expected.equals(runtime.activeId()),"Apply must only change pending selection");
                    require(originalSkin.equals(FModel.getPreferences().getPref(FPref.UI_SKIN)),"Appearance changed Classic skin preference");
                    Files.writeString(output.resolve("selected-theme.txt"),createdId);capture("09-pending");
                    Forge.openHomeDefault();phase=9;break;
                case 9:
                    require(!Forge.isMobileAdventureMode,"Classic mode not entered");
                    require(classicColor.equals(FSkinColor.get(FSkinColor.Colors.CLR_TEXT).getColor().toString()),"Classic palette leaked");
                    capture("10-classic-control");Forge.openAdventure();Forge.clearCurrentScreen();phase=10;break;
                case 10:
                    Controls.getSkin();require(expected.equals(runtime.activeId()),"Mode transition applied pending theme");capture("11-returned-adventure");
                    if(!Boolean.getBoolean("theme.fullScenes")){finish("THEME_UI_PASS");return;}
                    CampaignDefinition campaign=CampaignRegistry.discover().findById("fork.bellwake");require(campaign!=null,"Bellwake fixture missing");
                    CampaignManager.activate(campaign);WorldSave.beginPlaythrough(campaign,"Theme UI fixture","Theme UI fixture");
                    WorldSave.generateNewWorld("Theme UI fixture",true,0,0,ColorSet.fromNames("W"),Config.instance().getConfigData().difficulties[0],AdventureModes.Constructed,0,null,0);
                    require(WorldSave.saveInitialSnapshot(),WorldSave.getLastError());Forge.clearTransitionScreen();Forge.switchScene(GameScene.instance());phase=11;break;
                case 11:
                    capture("12-exploration");enter("Spawn");phase=12;break;
                case 12:
                    capture("13-interior-hud");Forge.switchScene(QuestLogScene.instance(Forge.getCurrentScene()));phase=13;break;
                case 13:
                    require(((Label)find(stage().getRoot(),"quest-list-heading")).getStyle().fontColor.equals(
                            Color.valueOf(runtime.resolveActive(Config.instance()).color("text.onPaper"))),"Journal heading needs its paper foreground");
                    capture("14-journal");Forge.switchScene(InventoryScene.instance());phase=14;break;
                case 14:
                    capture("15-inventory");Forge.switchScene(DeckEditScene.getInstance(previewRegion()));phase=15;break;
                case 15:
                    capture("16-deck");Forge.switchScene(ShopScene.instance(previewRegion()));phase=16;break;
                case 16:
                    capture("17-shop");
                    Array<Reward> rewards=new Array<>();rewards.add(new Reward(Reward.Type.Gold,120));rewards.add(new Reward(Reward.Type.Shards,2));
                    RewardScene.instance().loadRewards(rewards,RewardScene.Type.QuestReward,null);Forge.switchScene(RewardScene.instance());Forge.clearCurrentScreen();phase=17;break;
                case 17:
                    capture("18-reward");
                    UIScene reward=(UIScene)Forge.getCurrentScene();reward.showDialog(reward.createGenericDialog("Folio dialogue","A quiet place to prepare the next journey.","Continue",null,reward::removeDialog,null));phase=18;break;
                case 18:
                    capture("19-dialog");((UIScene)Forge.getCurrentScene()).removeDialog();
                    Forge.switchScene(TileMapScene.instance());
                    var opponent=MapStage.getInstance().getEnemyByID(61);require(opponent!=null,"Native duel fixture opponent missing");
                    DuelScene.instance().initDuels(MapStage.getInstance().getPlayerSprite(),opponent);
                    Forge.switchScene(DuelScene.instance());phase=19;break;
                case 19:
                    if(!(Forge.getCurrentScreen() instanceof forge.screens.match.MatchScreen match))return;
                    var overlay=forge.toolbox.FOverlay.getTopOverlay();
                    if(overlay instanceof forge.screens.match.CoinFlipOverlay coin){coin.tap(0,0,1);return;}
                    if(overlay instanceof forge.toolbox.FOptionPane option){capture("20-opening-dialog");option.setResult(0);return;}
                    if(overlay!=null)return;
                    var game=forge.screens.match.MatchController.instance.getGameView();
                    String ok=match.getActivePrompt().getBtnOk().getText();
                    System.out.println("THEME_MATCH phase="+(game==null?null:game.getPhase())+" ok="+ok);
                    if(ok.equals(Forge.getLocalizer().getMessage("lblPlay"))||ok.equals(Forge.getLocalizer().getMessage("lblKeep"))){match.getActivePrompt().getBtnOk().trigger();return;}
                    if(game==null||game.getPhase()==null)return;
                    if(ok.isBlank()||++readyMatchFrames<5)return;
                    capture("20-duel");require(expected.equals(runtime.activeId()),"A new gameplay scene applied pending theme");
                    forge.screens.match.TargetingOverlay.updateColors();
                    forge.toolbox.FOverlay targets=new forge.toolbox.FOverlay(FSkinColor.getStandardColor(new Color(0,0,0,0))){
                        @Override protected void doLayout(float width,float height){ }
                        @Override public void draw(forge.Graphics graphics){
                            var connections=new forge.screens.match.TargetingOverlay.ArcConnection[]{
                                    forge.screens.match.TargetingOverlay.ArcConnection.Friends,
                                    forge.screens.match.TargetingOverlay.ArcConnection.FoesBlocking,
                                    forge.screens.match.TargetingOverlay.ArcConnection.FoesAttacking};
                            for(int i=0;i<connections.length;i++)forge.screens.match.TargetingOverlay.drawArrow(graphics,
                                    new Vector2(getWidth()*(.3f+i*.2f),getHeight()*.7f),new Vector2(getWidth()*(.3f+i*.2f),getHeight()*.3f),connections[i]);
                        }
                    };
                    targets.show();capture("21-targeting-palette-sample");targets.hide();finish("THEME_FULL_SCENES_PASS");break;
                default:break;
            }
        }catch(Throwable error){error.printStackTrace();System.err.println("THEME_SMOKE_FAIL phase="+phase);System.exit(1);}
    }
    private boolean inputProbe(AdventureThemeRuntime runtime)throws Exception{
        String probe=System.getProperty("theme.inputProbe","");
        if(probe.equals("back")){
            UIScene scene=(UIScene)Forge.getCurrentScene();SelectBox<?> choices=find(stage().getRoot(),"theme-preset");
            stage().setKeyboardFocus(choices);choices.showScrollPane();stage().keyDown(Input.Keys.ESCAPE);
            require(Forge.getCurrentScene()==scene&&!choices.getList().isTouchable(),"Back must close an open choice list first");
            scene.showDialog(scene.createGenericDialog("Back precedence","Keep the workshop open","Close",null,scene::removeDialog,null));
            stage().keyDown(Input.Keys.BUTTON_B);require(Forge.getCurrentScene()==scene&&scene.dialogs.isEmpty(),"Back must dismiss the dialog before leaving its scene");
            stage().keyDown(Input.Keys.ESCAPE);require(Forge.getCurrentScene() instanceof AppearanceScene,"Escape must navigate Back from Workshop");
            stage().keyDown(Input.Keys.BUTTON_B);require(Forge.getCurrentScene() instanceof SettingsScene,"Controller Back must navigate from Appearance");
            finish("THEME_BACK_PASS");return true;
        }
        if(probe.equals("slider")){
            Slider slider=find(stage().getRoot(),"theme-texture");stage().setKeyboardFocus(slider);float before=slider.getValue();
            stage().keyDown(Input.Keys.RIGHT);require(slider.getValue()>before,"Focused texture slider ignores keyboard/controller right");
            stage().keyDown(Input.Keys.LEFT);require(Math.abs(slider.getValue()-before)<.001,"Texture slider left does not reverse one step");
            finish("THEME_SLIDER_PASS");return true;
        }
        if(probe.equals("keyboard")){
            UIScene scene=(UIScene)Forge.getCurrentScene();
            ScrollPane editor=find(stage().getRoot(),"theme-editor");
            for(String control:new String[]{"theme-name","theme-color-action.primary","theme-color-border.frameText","theme-ornament","theme-texture","theme-name"}){
                Actor target=find(stage().getRoot(),control);
                for(UIScene.Selectable selection:scene.getPossibleSelection())if(selection.actor==target){scene.selectActor(selection);break;}
                editor.updateVisualScroll();capture("04-controller-focus-"+control);
                Vector2 point=target.localToAscendantCoordinates(editor,new Vector2());
                require(point.y>=-.1f&&point.y+target.getHeight()<=editor.getHeight()+.1f,"Controller focus hides "+control+" at "+point);
            }
            TextField field=find(stage().getRoot(),"theme-name");
            scene.selectActor(new UIScene.Selectable<>(field));stage().keyDown(Input.Keys.BUTTON_A);
            require(scene.dialogs.peek() instanceof KeyBoardDialog,"Controller confirm did not open the existing keyboard");
            KeyBoardDialog keyboard=(KeyBoardDialog)scene.dialogs.peek();keyboard.setText("Controller edited recipe");
            var accept=KeyBoardDialog.class.getDeclaredMethod("setKeyboardDialogText");accept.setAccessible(true);accept.invoke(keyboard);
            click(find(stage().getRoot(),"theme-save-as"));Set<String> added=ids(runtime.discover());added.removeAll(beforeThemes);
            require(added.size()==1,"Controller edit did not save one recipe");
            String saved=added.iterator().next();require(ThemeResolver.resolve(runtime.discover(),saved).name().equals("Controller edited recipe"),"Keyboard completion saved the previous name");
            finish("THEME_KEYBOARD_PASS");return true;
        }
        return false;
    }
    private static java.lang.ref.WeakReference<Config> temporaryCampaignConfig(AdventureThemeRuntime runtime){
        Config temporary=Config.forCampaign(CampaignRegistry.discover().findById("fork.bellwake"),new forge.adventure.data.SettingData());
        runtime.resolveActive(temporary);return new java.lang.ref.WeakReference<>(temporary);
    }
    private void compatibilityProbe(AdventureThemeRuntime runtime)throws Exception{
        require(Config.instance().hasCustomThemeSkin(),"Bespoke fixture did not resolve its own skin");
        require(!(Controls.getSkin() instanceof ThemeSkinAdapter),"Automatic compatibility restyled a bespoke skin");
        require(Controls.getSkin().has("custom-theme-probe",Label.LabelStyle.class),"Campaign custom style was discarded");
        require(runtime.pendingId()==null,"Automatic migration persisted an explicit selection");
        AdventureThemeRuntime explicitRuntime=new AdventureThemeRuntime(Path.of(System.getProperty("theme.source"),"forge-gui/res/adventure/common/themes"),runtime.userRoot(),"folio",id->{});
        require(explicitRuntime.resolveActive(Config.instance())!=null,"Explicit theme fixture did not initialize");
        explicitRuntime.useCampaignPresentation(Config.instance(),"Deliberately incompatible campaign fixture");
        require(explicitRuntime.resolveActive(Config.instance())==null&&ThemePaletteBridge.presentationForAdventure()==null,"An incompatible campaign reactivated the shared theme palette");
        Skin independent=Controls.getSkin();independent.remove("white-pixel",TextureRegion.class);
        independent.remove("white-pixel",com.badlogic.gdx.scenes.scene2d.utils.Drawable.class);
        Skin explicit=ThemeSkinAdapter.adapt(independent,ThemeResolver.resolve(runtime.discover(),"folio"));
        require(explicit.getRegion("white-pixel").getTexture()!=null,"Independent themed atlas needs a real drawing pixel");explicit.dispose();
        Path limitedRoot=Files.createDirectories(runtime.userRoot().resolve("limited-font"));
        Pixmap page=new Pixmap(1,1,Pixmap.Format.RGBA8888);page.setColor(Color.WHITE);page.fill();
        try{PixmapIO.writePNG(Gdx.files.absolute(limitedRoot.resolve("page.png").toString()),page);}finally{page.dispose();}
        Files.writeString(limitedRoot.resolve("font.fnt"),"""
            info face=Fixture size=12 padding=0,0,0,0 spacing=0,0
            common lineHeight=12 base=10 scaleW=1 scaleH=1 pages=1 packed=0
            page id=0 file="page.png"
            chars count=1
            char id=65 x=0 y=0 width=1 height=1 xoffset=0 yoffset=0 xadvance=4 page=0 chnl=0
            """);
        Files.writeString(limitedRoot.resolve("theme.json"),"""
            {"schemaVersion":1,"id":"user.limited-font","name":"Limited font fixture","extends":"folio","fonts":{"body":"font.fnt"}}
            """);
        Skin fallback=ThemeSkinAdapter.adapt(independent,ThemeResolver.resolve(runtime.discover(),"user.limited-font"));
        require(fallback.getFont("default").getData().getGlyph('\u03a9')!=null,"Limited font erased the original Greek text coverage");
        require(!((ThemeSkinAdapter)fallback).warnings().isEmpty(),"Localized font fallback needs an author-facing notice");fallback.dispose();
        Forge.switchScene(new AppearanceScene());capture("02-campaign-appearance");
        require(((Button)find(stage().getRoot(),"theme-customize")).isDisabled(),"Campaign appearance cannot be edited as a recipe");
        Forge.switchScene(new ThemeWorkshopScene("folio"));capture("03-independent-workshop");
        require(find(stage().getRoot(),"swatch-action.primary")!=null,"Independent editor swatch missing");
        finish("THEME_COMPATIBILITY_PASS");
    }
    private void assetProbe(AdventureThemeRuntime runtime)throws Exception{
        Path root=Files.createDirectories(runtime.userRoot().resolve("asset-probe"));
        String[] names={"tab","resource","glass"},hex={"#9f3040","#3860d6","#e4c04d"};
        for(int i=0;i<names.length;i++){
            Pixmap pixel=new Pixmap(1,1,Pixmap.Format.RGBA8888);pixel.setColor(Color.valueOf(hex[i]));pixel.fill();
            try{PixmapIO.writePNG(Gdx.files.absolute(root.resolve(names[i]+".png").toString()),pixel);}finally{pixel.dispose();}
        }
        Files.writeString(root.resolve("tab.atlas"),"""
            tab.png
            size: 1,1
            format: RGBA8888
            filter: Nearest,Nearest
            repeat: none
            tab
              rotate: false
              xy: 0,0
              size: 1,1
              orig: 1,1
              offset: 0,0
              index: -1
            """);
        Files.writeString(root.resolve("theme.json"),"""
            {"schemaVersion":1,"id":"user.asset-probe","name":"Native material probe","extends":"folio","colors":{"surface.paper":"#101010","text.onPaper":"#eeeeee","phase.activeDisabled":"#101010"},"materials":{"tab":"tab.atlas","resource":"resource.png","glass":"glass.png"}}
            """);
        ResolvedTheme recipe=ThemeResolver.resolve(runtime.discover(),"user.asset-probe");
        ThemePreview preview=new ThemePreview(Controls.getThemePreviewSkin());preview.rebuild(recipe,ThemePreview.PreviewPage.COMPONENTS);
        preview.setBounds(8,8,Scene.getIntendedWidth()-16,Scene.getIntendedHeight()-16);stage().addActor(preview);
        capture("02-authored-tab-glass-frame");
        Actor tab=caption(preview,"Tab sample");require(tab!=null,"Declared tab sample missing");
        verifyPoint(tab,3,3,Color.valueOf(hex[0]));
        verifyPoint(preview,10,preview.getHeight()-2.6f,Color.valueOf(hex[2]));
        require(((Label)preview.findActor("preview-frame-caption")).getStyle().fontColor.equals(Color.valueOf(recipe.color("border.frameText"))),"Frame caption ignores its paired foreground");
        preview.rebuild(recipe,ThemePreview.PreviewPage.ADVENTURE);capture("03-authored-resources");
        Actor resources=preview.findActor("preview-life").getParent();verifyPoint(resources,resources.getWidth()-9,7,Color.valueOf(hex[1]));
        Table paper=new Table();paper.setBackground(new ThemeDrawable(recipe,Controls.getSkin().getRegion("white-pixel"),"paper",null));
        var ink=Controls.newTextraLabel("[BLACK]Journal ink \u03a9 [RED]Warning");paper.add(ink).pad(3);
        paper.setBounds(25,10,300,30);stage().addActor(paper);ThemePresentation.styleTree(paper,recipe,Controls.getSkin(),true);
        require(ink.storedText.contains("[#eeeeee]")&&ink.storedText.contains("[RED]"),"Neutral paper ink must follow its theme while warning colors remain meaningful");
        capture("04-semantic-paper-ink");paper.remove();preview.remove();preview.dispose();
        require(Controls.getThemePreviewSkin().getRegion("white-pixel").getTexture().getTextureObjectHandle()!=0,"Authored previews disposed a borrowed texture");
        finish("THEME_AUTHORED_ASSETS_PASS");
    }
    private void verifyPoint(Actor actor,float x,float y,Color expected)throws Exception{
        Vector2 point=stage().stageToScreenCoordinates(actor.localToStageCoordinates(new Vector2(x,y)));
        int px=Math.round(point.x*Gdx.graphics.getBackBufferWidth()/Gdx.graphics.getWidth());
        int py=Gdx.graphics.getBackBufferHeight()-1-Math.round(point.y*Gdx.graphics.getBackBufferHeight()/Gdx.graphics.getHeight());
        Pixmap pixel=Pixmap.createFromFrameBuffer(px,py,1,1);
        try{Color actual=new Color(pixel.getPixel(0,0));require(Math.abs(actual.r-expected.r)<.02&&Math.abs(actual.g-expected.g)<.02&&Math.abs(actual.b-expected.b)<.02,"Declared material was not drawn: "+actual+" vs "+expected);}
        finally{pixel.dispose();}
    }
    private static TextureRegion previewRegion(){return Forge.lastPreview==null?null:new TextureRegion(Forge.lastPreview);}
    private void capture(String name)throws Exception{
        Gdx.app.getApplicationListener().render(); // Capture the real screen hierarchy, including dialogs and the match.
        Pixmap shot=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
        try{PixmapIO.writePNG(Gdx.files.absolute(output.resolve(name+".png").toString()),shot,6,true);}finally{shot.dispose();}
    }
    private Stage stage()throws Exception{var field=UIScene.class.getDeclaredField("stage");field.setAccessible(true);return (Stage)field.get(Forge.getCurrentScene());}
    private void bounds(String name)throws Exception{
        Actor actor=find(stage().getRoot(),name);require(actor!=null,"Missing control: "+name);
        Vector2 p=actor.localToStageCoordinates(new Vector2());
        require(p.x>=-.1&&p.y>=-.1&&p.x+actor.getWidth()<=stage().getWidth()+.1&&p.y+actor.getHeight()<=stage().getHeight()+.1,"Control outside viewport: "+name+" at "+p+" size "+actor.getWidth()+"x"+actor.getHeight());
    }
    private void previewContained()throws Exception{
        Actor preview=find(stage().getRoot(),"theme-workshop-preview"),title=find(stage().getRoot(),"preview-title");
        Vector2 p=title.localToAscendantCoordinates((Group)preview,new Vector2());
        require(p.y>=-.1&&p.y+title.getHeight()<=preview.getHeight()+.1,"Preview title overflows its material frame");
    }
    private void verifySwatch(String role)throws Exception{
        Actor swatch=find(stage().getRoot(),"swatch-"+role);TextField field=find(stage().getRoot(),"theme-color-"+role);
        Vector2 point=stage().stageToScreenCoordinates(swatch.localToStageCoordinates(new Vector2(swatch.getWidth()/2,swatch.getHeight()/2)));
        int x=Math.round(point.x*Gdx.graphics.getBackBufferWidth()/Gdx.graphics.getWidth());
        int y=Gdx.graphics.getBackBufferHeight()-1-Math.round(point.y*Gdx.graphics.getBackBufferHeight()/Gdx.graphics.getHeight());
        Pixmap pixel=Pixmap.createFromFrameBuffer(x,y,1,1);
        try{
            Color actual=new Color(pixel.getPixel(0,0)),expected=Color.valueOf(field.getText());
            require(Math.abs(actual.r-expected.r)<.02&&Math.abs(actual.g-expected.g)<.02&&Math.abs(actual.b-expected.b)<.02,
                    "Swatch differs from entered color: "+actual+" vs "+expected);
        }finally{pixel.dispose();}
    }
    @SuppressWarnings("unchecked") private static <T extends Actor>T find(Group root,String name){return (T)root.findActor(name);}
    private static void click(Actor actor){
        require(actor!=null,"Missing action");
        if(actor instanceof TextButton button){button.setChecked(!button.isChecked());return;}
        for(EventListener listener:new Array<>(actor.getListeners()))if(listener instanceof ClickListener click)click.clicked(null,0,0);
    }
    private void clickCaption(String text)throws Exception{
        Actor button=caption(stage().getRoot(),text);require(button!=null,"Missing native button: "+text);click(button);
    }
    private static Actor caption(Actor actor,String text){
        if(actor instanceof TextButton button&&button.getText().toString().equals(text))return actor;
        if(actor instanceof Group group)for(Actor child:group.getChildren()){Actor result=caption(child,text);if(result!=null)return result;}
        return null;
    }
    private static void type(TextField field,String text){field.setText(text);field.fire(new ChangeListener.ChangeEvent());}
    @SuppressWarnings({"rawtypes","unchecked"}) private static void selectId(SelectBox chooser,String id)throws Exception{
        for(Object choice:chooser.getItems()){
            var method=choice.getClass().getDeclaredMethod("id");method.setAccessible(true);
            if(id.equals(method.invoke(choice))){chooser.setSelected(choice);return;}
        }
        throw new AssertionError("New theme missing from chooser: "+id);
    }
    private static Set<String> ids(ThemeCatalog catalog){Set<String> ids=new HashSet<>();for(ThemeManifest theme:catalog.themes())ids.add(theme.id());return ids;}
    private static void enter(String name){
        var point=Current.world().getAllPointOfInterest().stream().filter(p->p.getData().name.equals(name)).findFirst().orElseThrow();
        TileMapScene.instance().load(point);Forge.switchScene(TileMapScene.instance());
    }
    private void finish(String marker)throws Exception{System.out.println(marker);Files.writeString(output.resolve("PASS"),marker+"\nactive="+AdventureThemeRuntime.current().activeId());phase=100;Gdx.app.exit();}
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
