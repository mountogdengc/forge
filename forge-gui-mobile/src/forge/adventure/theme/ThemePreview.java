package forge.adventure.theme;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Disposable;
import java.util.function.BiFunction;

/** Local sample actors; no game state, card database, global font cache, or settings. */
public final class ThemePreview extends Table implements Disposable {
    public enum PreviewPage { COMPONENTS, ADVENTURE, DUEL }
    private final Skin legacy;
    private final BiFunction<String,String,String> localize;
    private Skin adapted;
    public ThemePreview(Skin legacy){this(legacy,(key,fallback)->fallback);}
    public ThemePreview(Skin legacy,BiFunction<String,String,String> localize){this.legacy=legacy;this.localize=localize;}
    private String text(String key,String fallback){return localize.apply(key,fallback);}
    public void rebuild(ThemeDraft draft,PreviewPage page){
        rebuild(draft.resolve(),page); // Invalid edits leave the current valid preview intact.
    }
    public void rebuild(ResolvedTheme resolved,PreviewPage page){
        Skin replacement;
        try{replacement=ThemeSkinAdapter.adapt(legacy,resolved);}
        catch(RuntimeException failure){throw new IllegalArgumentException("Theme preview could not load its artwork or fonts: "+failure.getMessage(),failure);}
        if(replacement==legacy)throw new IllegalArgumentException(ThemeSkinAdapter.compatibilityIssue(legacy));
        clearChildren();if(adapted!=null&&adapted!=legacy)adapted.dispose();adapted=replacement;
        setBackground(adapted.getDrawable("theme.panel"));pad(6);defaults().pad(2);
        Label title=new Label(resolved.name(),new Label.LabelStyle(adapted.getFont("theme.heading"),Color.valueOf(resolved.color("text.primary"))));
        title.setName("preview-title");title.setWrap(true);add(title).growX().minWidth(0);row();
        switch(page){case COMPONENTS->components(resolved);case ADVENTURE->adventure(resolved);case DUEL->duel(resolved);}
        invalidateHierarchy();
    }
    private Label label(String text){return new Label(text,adapted);}
    private void components(ResolvedTheme theme){
        Table buttons=new Table();buttons.defaults().pad(2).growX();
        TextButton primary=new TextButton(text("lblThemePreviewPrimary","Primary action"),adapted,"theme.primary");
        TextButton secondary=new TextButton(text("lblThemePreviewSecondary","Secondary action"),adapted);
        TextButton selected=new TextButton(text("lblThemePreviewSelected","Selected"),adapted);selected.setChecked(true);
        TextButton disabled=new TextButton(text("lblThemePreviewUnavailable","Unavailable"),adapted);disabled.setDisabled(true);
        buttons.add(primary);buttons.add(secondary);buttons.row();buttons.add(selected);buttons.add(disabled);
        add(buttons).growX();row();
        Table frame=new Table();frame.setBackground(new ThemeDrawable(theme,adapted.getRegion("white-pixel"),"frame",null));
        Label inscription=new Label(text("lblThemePreviewFrame","Frame inscription"),new Label.LabelStyle(adapted.getFont("default"),Color.valueOf(theme.color("border.frameText"))));
        inscription.setName("preview-frame-caption");frame.add(inscription).pad(3);
        frame.add(new TextButton(text("lblThemePreviewTab","Tab sample"),adapted,"theme.tab")).pad(2);
        add(frame).growX();row();
        paper(text("lblThemePreviewPaper","A page from the folio. Quiet surfaces keep rules and choices readable."),theme);
    }
    private void adventure(ResolvedTheme theme){
        Table resources=new Table();resources.setBackground(adapted.getDrawable("theme.resource"));resources.defaults().pad(2).left();
        Label life=label(text("lblThemePreviewLife","Life 20 / 20"));life.setName("preview-life");
        resources.add(life);resources.add(label(text("lblThemePreviewGold","Gold 635")));resources.row();
        resources.add(label(text("lblThemePreviewShards","Shards 8")));resources.add(label(text("lblThemePreviewKeys","Keys 2")));
        add(resources).growX();row();paper(text("lblThemePreviewJournal","Journal: Follow the river to the watchtower."),theme);
    }
    private void duel(ResolvedTheme theme){
        add(label(text("lblThemePreviewOpponent","Opponent: Life 18"))).left();row();
        Table phases=new Table();String[] captions={text("lblThemePreviewUpkeep","Upkeep"),text("lblThemePreviewDraw","Draw"),text("lblThemePreviewMain","Main"),text("lblThemePreviewCombat","Combat")};
        for(int i=0;i<captions.length;i++){
            boolean current=i==2;Table cell=new Table();cell.setBackground(new ThemeDrawable(theme,adapted.getRegion("white-pixel"),current?"phase.activeEnabled":"phase.inactiveEnabled",null));
            Label value=new Label(captions[i],new Label.LabelStyle(adapted.getFont("default"),ThemePaletteBridge.phaseText(theme,current,true)));
            cell.add(value).pad(2);phases.add(cell).growX().pad(1);
        }
        add(phases).growX();row();add(label(text("lblThemePreviewYourTurn","Your turn: Life 20"))).left();row();
        add(new TextButton(text("lblThemePreviewCombatAction","To combat"),adapted,"theme.primary")).growX();
    }
    private void paper(String content,ResolvedTheme theme){
        Table paper=new Table();paper.setBackground(adapted.getDrawable("theme.paper"));
        Label body=new Label(content,new Label.LabelStyle(adapted.getFont("default"),Color.valueOf(theme.color("text.onPaper"))));body.setWrap(true);
        paper.add(body).pad(5).growX().minWidth(0);add(paper).growX().minWidth(0);
    }
    public java.util.List<String> warnings(){return adapted instanceof ThemeSkinAdapter adapter?adapter.warnings():java.util.List.of();}
    @Override public void draw(com.badlogic.gdx.graphics.g2d.Batch batch,float parentAlpha){
        super.draw(batch,parentAlpha);
        if(adapted!=null)adapted.getDrawable("theme.glass").draw(batch,getX(),getY(),getWidth(),getHeight());
    }
    @Override public void dispose(){clearChildren();setBackground((com.badlogic.gdx.scenes.scene2d.utils.Drawable)null);if(adapted!=null&&adapted!=legacy)adapted.dispose();adapted=null;}
}
