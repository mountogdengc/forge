package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import forge.Forge;
import forge.adventure.theme.*;
import forge.adventure.util.Controls;

/** Adventure-only selection. Applying records a pending choice, never a restart. */
public final class AppearanceScene extends AppearanceBaseScene {
    private final AdventureThemeRuntime runtime;
    private final Skin skin;
    private final SelectBox<AdventureAppearanceUi.Choice> chooser;
    private final ThemePreview preview;
    private final Label active,pending,message;
    private final TextButton customize,apply;
    private boolean binding,disposed;
    public AppearanceScene(){
        super(Forge.isLandscapeMode()?"ui/appearance.json":"ui/appearance_portrait.json");
        runtime=AdventureThemeRuntime.current();skin=Controls.getThemeEditorSkin();
        if(runtime==null)throw new IllegalStateException("Adventure appearance is not initialized");
        Table root=new Table();root.setFillParent(true);root.pad(9);root.defaults().pad(2);
        if(skin.has("theme.panel",com.badlogic.gdx.scenes.scene2d.utils.Drawable.class))root.setBackground(skin.getDrawable("theme.panel"));
        root.add(AdventureAppearanceUi.label(AdventureAppearanceUi.text("lblAdventureAppearance","Adventure appearance"),skin)).growX().minWidth(0);root.row();
        active=AdventureAppearanceUi.label("",skin);active.setName("theme-active");pending=AdventureAppearanceUi.label("",skin);pending.setName("theme-pending");
        root.add(active).growX().minWidth(0);root.row();root.add(pending).growX().minWidth(0);root.row();
        chooser=new SelectBox<>(skin);chooser.setName("theme-choice");chooser.setMaxListCount(7);
        root.add(chooser).growX().minWidth(0).height(20);root.row();
        preview=new ThemePreview(Controls.getThemePreviewSkin(),AdventureAppearanceUi::text);preview.setName("theme-preview");
        ScrollPane previewScroll=new ScrollPane(preview,skin);previewScroll.setScrollingDisabled(true,false);
        previewScroll.setFadeScrollBars(false);
        root.add(previewScroll).grow().minWidth(0).minHeight(75);root.row();
        message=AdventureAppearanceUi.label("",skin);message.setName("theme-message");
        ScrollPane messagePane=new ScrollPane(message,skin);messagePane.setScrollingDisabled(true,false);
        root.add(messagePane).growX().minWidth(0).height(32);root.row();
        Table buttons=new Table();buttons.defaults().pad(2).growX().minWidth(0).height(Forge.isLandscapeMode()?34:46);
        TextButton back=AdventureAppearanceUi.button(AdventureAppearanceUi.text("lblBack","Back"),skin,this::back,false);
        customize=AdventureAppearanceUi.button(AdventureAppearanceUi.text("lblThemeCustomize","Customize"),skin,()->{
            if(chooser.getSelected()!=null)Forge.switchScene(new ThemeWorkshopScene(chooser.getSelected().id()));
        },false);customize.setName("theme-customize");
        apply=AdventureAppearanceUi.button(AdventureAppearanceUi.text("lblThemeApplyAfterRestart","Apply after restart"),skin,this::apply,true);apply.setName("theme-apply");
        buttons.add(back);buttons.add(customize);buttons.add(apply);root.add(buttons).growX();
        stage.addActor(root);addToSelectable(chooser);addToSelectable(back);addToSelectable(customize);addToSelectable(apply);
        chooser.addListener(new ChangeListener(){@Override public void changed(ChangeEvent event,Actor actor){if(!binding)showChoice();}});
        refresh();
    }
    private void refresh(){
        binding=true;String id=chooser.getSelected()==null?runtime.pendingId():chooser.getSelected().id();
        chooser.setItems(AdventureAppearanceUi.choices(runtime,true));
        String desired=id==null?runtime.activeId():id;
        for(var c:chooser.getItems())if(c.id().equals(desired)){chooser.setSelected(c);break;}
        binding=false;active.setText(AdventureAppearanceUi.text("lblThemeActive","Active")+": "+AdventureAppearanceUi.name(runtime,runtime.activeId()));
        pending.setText(AdventureAppearanceUi.text("lblThemePending","Selected for next launch")+": "+AdventureAppearanceUi.name(runtime,runtime.pendingId()));
        showChoice();
    }
    private void showChoice(){
        var choice=chooser.getSelected();if(choice==null){apply.setDisabled(true);customize.setDisabled(true);return;}
        boolean legacy=choice.id().equals("campaign");customize.setDisabled(legacy);apply.setDisabled(false);
        if(legacy){preview.setVisible(false);message.setText(AdventureAppearanceUi.text("lblThemeLegacyHint","Uses the campaign's original appearance."));return;}
        try{
            preview.rebuild(ThemeResolver.resolve(runtime.discover(),choice.id()),ThemePreview.PreviewPage.COMPONENTS);preview.setVisible(true);
            String hint=legacy?AdventureAppearanceUi.text("lblThemeLegacyHint","Uses the campaign's original appearance.")
                    :AdventureAppearanceUi.text("lblThemeRestartHint","A full Forge restart is required. Your current run stays open.");
            if(!runtime.diagnostics().isEmpty())hint+="\n"+runtime.diagnostics().get(runtime.diagnostics().size()-1);
            if(!preview.warnings().isEmpty())hint+="\n"+preview.warnings().get(0);
            message.setText(hint);
        }catch(IllegalArgumentException e){message.setText(e.getMessage());apply.setDisabled(true);}
    }
    private void apply(){
        try{var selected=chooser.getSelected();if(selected==null)return;runtime.requestSelection(selected.id());
            pending.setText(AdventureAppearanceUi.text("lblThemePending","Selected for next launch")+": "+selected.label());
            message.setText(AdventureAppearanceUi.text("lblThemeSelectionSaved","Selection saved. Close and reopen Forge when you are ready."));
        }catch(IllegalArgumentException e){message.setText(e.getMessage());}
    }
    @Override public void enter(){refresh();super.enter();}
    @Override public boolean back(){Forge.switchToLast();Gdx.app.postRunnable(this::dispose);return true;}
    @Override public void dispose(){if(disposed)return;disposed=true;preview.dispose();stage.dispose();}
}
