package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import forge.Forge;
import forge.adventure.theme.*;
import forge.adventure.util.Controls;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Native local recipe editor with an independent, immediately updated preview. */
public final class ThemeWorkshopScene extends AppearanceBaseScene {
    private final AdventureThemeRuntime runtime;
    private final Skin skin;
    private final SelectBox<AdventureAppearanceUi.Choice> presets;
    private final SelectBox<String> ornaments;
    private final TextField name;
    private final Slider texture;
    private final Label status,textureValue;
    private final TextButton save;
    private final ThemePreview preview;
    private final Map<String,TextField> colors=new LinkedHashMap<>();
    private final Map<String,Image> swatches=new LinkedHashMap<>();
    private ThemeDraft draft;
    private ThemePreview.PreviewPage page=ThemePreview.PreviewPage.COMPONENTS;
    private boolean binding,disposed;
    public ThemeWorkshopScene(String baseId){
        super(Forge.isLandscapeMode()?"ui/theme_workshop.json":"ui/theme_workshop_portrait.json");
        runtime=AdventureThemeRuntime.current();skin=Controls.getThemeEditorSkin();
        draft=ThemeDraft.from(runtime.discover(),baseId);
        Table root=new Table();root.setFillParent(true);root.pad(8);root.defaults().pad(2);
        if(skin.has("theme.panel",com.badlogic.gdx.scenes.scene2d.utils.Drawable.class))root.setBackground(skin.getDrawable("theme.panel"));
        root.add(AdventureAppearanceUi.label(AdventureAppearanceUi.text("lblThemeWorkshop","Theme workshop"),skin)).growX().minWidth(0);root.row();
        presets=new SelectBox<>(skin);presets.setName("theme-preset");presets.setItems(AdventureAppearanceUi.choices(runtime,false));presets.setMaxListCount(6);
        for(var choice:presets.getItems())if(choice.id().equals(baseId)){presets.setSelected(choice);break;}
        root.add(presets).growX().minWidth(0).height(20);root.row();
        Table form=new Table();form.defaults().pad(2).minWidth(0);
        form.add(AdventureAppearanceUi.label(AdventureAppearanceUi.text("lblThemeName","Theme name"),skin)).growX().colspan(2);form.row();
        name=new TextField(draft.name(),skin);name.setProgrammaticChangeEvents(true);name.setName("theme-name");name.setMaxLength(120);form.add(name).growX().colspan(2).height(20);form.row();
        colorRow(form,"action.primary","lblThemeJewelAccent","Jewel accent");
        colorRow(form,"action.onPrimary","lblThemeAccentText","Accent text");
        colorRow(form,"border.frame","lblThemeFrameMaterial","Frame material");
        colorRow(form,"border.frameText","lblThemeFrameText","Frame text");
        form.add(AdventureAppearanceUi.label(AdventureAppearanceUi.text("lblThemeOrnament","Ornament"),skin)).growX();
        ornaments=new SelectBox<>(skin);ornaments.setName("theme-ornament");
        ornaments.setItems(AdventureAppearanceUi.text("lblThemeQuiet","Quiet"),AdventureAppearanceUi.text("lblThemeBalanced","Balanced"),AdventureAppearanceUi.text("lblThemeFull","Full"));
        form.add(ornaments).growX().height(20);form.row();
        textureValue=AdventureAppearanceUi.label("",skin);form.add(textureValue).growX().colspan(2);form.row();
        texture=new Slider(0,.35f,.01f,false,skin);texture.setName("theme-texture");form.add(texture).growX().colspan(2).minWidth(0).height(18);form.row();
        ScrollPane editorScroll=new ScrollPane(form,skin);editorScroll.setName("theme-editor");editorScroll.setScrollingDisabled(true,false);editorScroll.setFadeScrollBars(false);
        preview=new ThemePreview(Controls.getThemePreviewSkin(),AdventureAppearanceUi::text);preview.setName("theme-workshop-preview");
        Table previewArea=new Table();previewArea.defaults().pad(1).minWidth(0);
        Table tabs=new Table();tabs.defaults().growX().pad(1).minWidth(0).height(22);
        ButtonGroup<TextButton> tabGroup=new ButtonGroup<>();
        for(var p:ThemePreview.PreviewPage.values()){
            String key=switch(p){case COMPONENTS->"lblThemeComponents";case ADVENTURE->"lblAdventure";case DUEL->"lblDuel";};
            String fallback=switch(p){case COMPONENTS->"Components";case ADVENTURE->"Adventure";case DUEL->"Duel";};
            TextButton tab=new TextButton(AdventureAppearanceUi.text(key,fallback),skin,skin.has("theme.tab",TextButton.TextButtonStyle.class)?"theme.tab":"default");
            tabGroup.add(tab);
            tab.addListener(new ChangeListener(){@Override public void changed(ChangeEvent event,Actor actor){
                if(tab.isChecked()){page=p;updatePreview();}
            }});
            tabs.add(tab);addToSelectable(tab);
        }
        ScrollPane previewScroll=new ScrollPane(preview,skin);previewScroll.setScrollingDisabled(true,false);
        previewScroll.setFadeScrollBars(false);
        previewArea.add(tabs).growX();previewArea.row();previewArea.add(previewScroll).grow().minWidth(0).minHeight(80);
        Table body=new Table();body.defaults().pad(2).minWidth(0);
        if(Forge.isLandscapeMode()){
            body.add(editorScroll).grow().width(getIntendedWidth()*.46f);body.add(previewArea).grow().width(getIntendedWidth()*.46f);
        }else{
            body.add(previewArea).growX().minHeight(135);body.row();body.add(editorScroll).grow().minHeight(90);
        }
        root.add(body).grow().minHeight(130);root.row();
        status=AdventureAppearanceUi.label("",skin);status.setName("theme-validation");
        ScrollPane statusPane=new ScrollPane(status,skin);statusPane.setScrollingDisabled(true,false);
        root.add(statusPane).growX().minWidth(0).height(32);root.row();
        Table buttons=new Table();buttons.defaults().pad(2).growX().minWidth(0).height(Forge.isLandscapeMode()?34:46);
        TextButton back=AdventureAppearanceUi.button(AdventureAppearanceUi.text("lblBack","Back"),skin,this::back,false);
        TextButton restore=AdventureAppearanceUi.button(AdventureAppearanceUi.text("lblThemeRestore","Restore"),skin,()->{draft.restore();bind();},false);restore.setName("theme-restore");
        save=AdventureAppearanceUi.button(AdventureAppearanceUi.text("lblThemeSaveAsNew","Save as new theme"),skin,this::save,true);save.setName("theme-save-as");
        buttons.add(back);buttons.add(restore);buttons.add(save);root.add(buttons).growX();stage.addActor(root);stage.setScrollFocus(editorScroll);
        addToSelectable(presets);addToSelectable(name);for(TextField field:colors.values())addToSelectable(field);
        addToSelectable(ornaments);addToSelectable(texture);addToSelectable(back);addToSelectable(restore);addToSelectable(save);
        presets.addListener(change(()->{if(presets.getSelected()!=null){draft=ThemeDraft.from(runtime.discover(),presets.getSelected().id());bind();}}));
        name.addListener(change(()->{draft.setName(name.getText());updatePreview();}));
        ornaments.addListener(change(()->{draft.setOrnament(new String[]{"quiet","balanced","full"}[ornaments.getSelectedIndex()]);updatePreview();}));
        texture.addListener(change(()->{draft.setTexture(texture.getValue());updatePreview();}));
        bind();
    }
    private ChangeListener change(Runnable action){return new ChangeListener(){@Override public void changed(ChangeEvent event,Actor actor){if(!binding)action.run();}};}
    private void colorRow(Table form,String role,String key,String fallback){
        form.add(AdventureAppearanceUi.label(AdventureAppearanceUi.text(key,fallback),skin)).growX();
        Table entry=new Table();Image swatch=new Image(ThemePresentation.swatchDrawable(skin));swatch.setName("swatch-"+role);entry.add(swatch).size(12).padRight(3);
        TextField field=new TextField(draft.color(role),skin);field.setProgrammaticChangeEvents(true);field.setName("theme-color-"+role);field.setMaxLength(7);
        field.setTextFieldFilter((f,c)->c=='#'||Character.digit(c,16)>=0);entry.add(field).growX().minWidth(0).height(20);
        colors.put(role,field);swatches.put(role,swatch);form.add(entry).growX().minWidth(0).width(88);form.row();
        field.addListener(change(()->{
            String value=field.getText();draft.setColor(role,value);
            if(role.equals("action.primary")){draft.setColor("state.selected",value);draft.setColor("phase.activeEnabled",value);}
            if(role.equals("action.onPrimary"))draft.setColor("state.onSelected",value);
            updatePreview();
        }));
    }
    private void bind(){
        binding=true;name.setText(draft.name());colors.forEach((role,field)->field.setText(draft.color(role)));
        ornaments.setSelectedIndex(switch(draft.ornament()){case "quiet"->0;case "balanced"->1;default->2;});texture.setValue(draft.texture());binding=false;
        updatePreview();
    }
    private void updatePreview(){
        textureValue.setText(AdventureAppearanceUi.text("lblThemePaperTexture","Paper texture")+": "+Math.round(texture.getValue()*100)+"%");
        colors.forEach((role,field)->{if(field.getText().matches("#[0-9a-fA-F]{6}"))swatches.get(role).setColor(Color.valueOf(field.getText()));});
        var errors=draft.validate();save.setDisabled(!errors.isEmpty());
        if(!errors.isEmpty()){status.setText(errors.get(0));return;}
        try{preview.rebuild(draft,page);String hint=AdventureAppearanceUi.text("lblThemePreviewOnly","Preview only. Save creates a new theme; your active theme stays unchanged.");
            if(!preview.warnings().isEmpty())hint+="\n"+preview.warnings().get(0);
            status.setText(hint);}
        catch(IllegalArgumentException e){status.setText(e.getMessage());save.setDisabled(true);}
    }
    private void save(){
        try{
            Path file=ThemeStore.saveAsNew(runtime.userRoot(),runtime.discover(),draft.toVariant("user.draft"));
            ThemeManifest saved=ThemeManifest.parse(file,Files.readString(file));
            status.setText(AdventureAppearanceUi.text("lblThemeCreated","Theme created")+": "+saved.name()+". "+AdventureAppearanceUi.text("lblThemeChooseToApply","Select it in Appearance to apply after restart."));
        }catch(Exception e){status.setText(e.getMessage());}
    }
    @Override public boolean back(){Forge.switchToLast();Gdx.app.postRunnable(this::dispose);return true;}
    @Override public void dispose(){if(disposed)return;disposed=true;preview.dispose();stage.dispose();}
}
