package forge.adventure.theme;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.util.Config;
import java.util.*;

/** Explicit presentation slots; campaign layout and gameplay artwork are never resolved here. */
public final class ThemePresentation {
    private ThemePresentation(){ }
    private static final Set<String> HUD=Set.of("hud.json","hud_portrait.json","hud_landscape.json");
    private static final Set<String> PAPER_LABELS=Set.of("item_description","totalWins","wins","totalLoss","loss","lossWinRatio","winloss","playerName");
    public static Optional<Drawable> drawableFor(String uiPath,String actorName,String legacyAsset,Skin adaptedSkin){
        if(!(adaptedSkin instanceof ThemeSkinAdapter)||!HUD.contains(uiPath)||legacyAsset==null)return Optional.empty();
        String slot=null;
        if("mapborder".equals(actorName)&&legacyAsset.equals("ui/minimap.png"))slot="minimap";
        if("avatarborder".equals(actorName)&&legacyAsset.equals("ui/avatarhud.png"))slot="medallion";
        if("gamehud".equals(actorName)&&Set.of("ui/hud.png","ui/hud_portrait.png").contains(legacyAsset))slot="hud";
        return slot==null?Optional.empty():Optional.of(slot.equals("hud")
                ?hudBacking(adaptedSkin.getDrawable("theme.hud"),adaptedSkin.getDrawable("theme.resource"))
                :adaptedSkin.getDrawable("theme."+slot));
    }
    public static Drawable chromeFor(Skin skin){return skin instanceof ThemeSkinAdapter?skin.getDrawable("theme.glass"):null;}
    static Drawable hudBacking(Drawable hud,Drawable resource){
        return new BaseDrawable(hud){
            @Override public void draw(com.badlogic.gdx.graphics.g2d.Batch batch,float x,float y,float width,float height){
                hud.draw(batch,x,y,width,height);
                resource.draw(batch,x+2,y+2,Math.max(0,width-4),Math.max(0,height-4));
            }
        };
    }
    public static ResolvedTheme activeTheme(){
        AdventureThemeRuntime runtime=AdventureThemeRuntime.current();
        return Forge.isMobileAdventureMode&&runtime!=null?runtime.resolveActive(Config.instance()):null;
    }
    public static Color activeColor(String role,Color fallback){ResolvedTheme theme=activeTheme();return theme==null?fallback:Color.valueOf(theme.color(role));}
    public static Drawable swatchDrawable(Skin skin){
        return skin.getDrawable("white-pixel");
    }
    public static void style(Actor actor,String uiPath,String actorName){ResolvedTheme theme=activeTheme();if(theme!=null)style(actor,uiPath,actorName,theme);}
    public static void style(Actor actor,String uiPath,String actorName,ResolvedTheme theme){
        if(actorName!=null&&PAPER_LABELS.contains(actorName)&&
                (uiPath.startsWith("inventory")||uiPath.startsWith("statistic")))text(actor,theme,"text.onPaper");
    }
    public static void paperText(Actor actor){ResolvedTheme theme=activeTheme();if(theme!=null)styleTree(actor,theme,forge.adventure.util.Controls.getSkin(),true);}
    public static void styleTree(Actor actor){ResolvedTheme theme=activeTheme();if(theme!=null)styleTree(actor,theme,forge.adventure.util.Controls.getSkin(),false);}
    public static void styleTree(Actor actor,ResolvedTheme theme,Skin adaptedSkin,boolean paper){
        if(adaptedSkin instanceof ThemeSkinAdapter adapter)tree(actor,theme,adapter,paper);
    }
    private static void tree(Actor actor,ResolvedTheme theme,ThemeSkinAdapter adapter,boolean paper){
        if(actor instanceof Button)return; // Captions retain their action foreground pair.
        if(actor instanceof Window window&&window.getStyle().background instanceof ThemeDrawable)paper=true;
        if(actor instanceof Table table&&table.getBackground() instanceof ThemeDrawable d)paper=d.isPaper();
        if(actor instanceof ScrollPane scroll&&scroll.getStyle().background instanceof ThemeDrawable d)paper=d.isPaper();
        if(actor instanceof Label label&&label.getStyle().background instanceof ThemeDrawable d)paper=d.isPaper();
        if(actor instanceof TextraLabel label&&label.style!=null&&label.style.background instanceof ThemeDrawable d)paper=d.isPaper();
        boolean owned=actor instanceof Label nativeLabel&&adapter.ownsFont(nativeLabel.getStyle().font)
                ||actor instanceof TextraLabel textraLabel&&textraLabel.style!=null&&adapter.ownsFont(textraLabel.style.font);
        Color tint=actor.getColor();
        boolean neutral=tint.r==tint.g&&tint.g==tint.b;
        if(owned&&neutral)text(actor,theme,paper?"text.onPaper":"text.primary");
        if(actor instanceof Group group)for(Actor child:group.getChildren())tree(child,theme,adapter,paper);
    }
    static String paperMarkup(String content,ResolvedTheme theme,String role){
        return role.equals("text.onPaper")?content.replace("[BLACK]","["+theme.color(role)+"]"):content;
    }
    private static void text(Actor actor,ResolvedTheme theme,String role){
        if(actor instanceof Label label){Label.LabelStyle copy=new Label.LabelStyle(label.getStyle());copy.fontColor=Color.valueOf(theme.color(role));label.setStyle(copy);label.setColor(1,1,1,label.getColor().a);}
        else if(actor instanceof TextraLabel label){label.layout.setBaseColor(Color.valueOf(theme.color(role)));label.setColor(1,1,1,label.getColor().a);label.setText(paperMarkup(label.storedText,theme,role));}
    }
}
