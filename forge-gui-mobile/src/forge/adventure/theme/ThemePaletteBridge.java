package forge.adventure.theme;

import com.badlogic.gdx.graphics.Color;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkin;
import forge.assets.FSkinColor;
import forge.util.Utils;
import java.util.Map;

/** The custom renderer and Scene2D share the same active, immutable recipe. */
public final class ThemePaletteBridge {
    private ThemePaletteBridge(){ }
    private static ResolvedTheme active;
    private static final Map<String,String> ROLES=Map.ofEntries(
            Map.entry("ADV_CLR_THEME","surface.canvas"),Map.entry("ADV_CLR_THEME2","surface.panel"),
            Map.entry("ADV_CLR_BORDERS","border.frame"),Map.entry("ADV_CLR_ZEBRA","surface.panel"),
            Map.entry("ADV_CLR_HOVER","state.hover"),Map.entry("ADV_CLR_ACTIVE","state.selected"),
            Map.entry("ADV_CLR_INACTIVE","state.disabled"),Map.entry("ADV_CLR_TEXT","text.primary"),
            Map.entry("ADV_CLR_OVERLAY","surface.canvas"),Map.entry("ADV_CLR_PHASE_ACTIVE_ENABLED","phase.activeEnabled"),
            Map.entry("ADV_CLR_PHASE_ACTIVE_DISABLED","phase.activeDisabled"),Map.entry("ADV_CLR_PHASE_INACTIVE_ENABLED","phase.inactiveEnabled"),
            Map.entry("ADV_CLR_PHASE_INACTIVE_DISABLED","phase.inactiveDisabled"),Map.entry("ADV_CLR_NORMAL_TARGETING_ARROW","target.normal"),
            Map.entry("ADV_CLR_COMBAT_TARGETING_ARROW","target.combat"),Map.entry("ADV_CLR_PWATTK_TARGETING_ARROW","target.planeswalker"));
    public static void activate(ResolvedTheme theme){
        if(active==theme)return;active=theme;
        if(FSkin.isLoaded())FSkinColor.updateAll();
    }
    public static ResolvedTheme presentationForAdventure(){return Forge.isMobileAdventureMode?active:null;}
    public static Color colorFor(ResolvedTheme theme,String role,Color legacy,boolean adventure){
        String semantic=ROLES.get(role);
        return adventure&&theme!=null&&semantic!=null?Color.valueOf(theme.color(semantic)):legacy;
    }
    public static Color adventureColor(FSkinColor.Colors role,Color legacy){return colorFor(active,role.name(),legacy,Forge.isMobileAdventureMode);}
    public static Color phaseFill(ResolvedTheme theme,boolean current,boolean stop){
        return Color.valueOf(theme.color("phase."+(current?"active":"inactive")+(stop?"Enabled":"Disabled")));
    }
    public static Color phaseText(ResolvedTheme theme,boolean current,boolean stop){
        return Color.valueOf(theme.color(current?(stop?"action.onPrimary":"text.onPaper"):"text.primary"));
    }
    public static Color progressColor(ResolvedTheme theme,boolean selected,boolean foreground,Color legacy,boolean adventure){
        if(!adventure||theme==null)return legacy;
        return Color.valueOf(theme.color(selected?(foreground?"action.onPrimary":"action.primary"):(foreground?"text.primary":"surface.panel")));
    }
    public static Color progressColor(boolean selected,boolean foreground,Color legacy){return progressColor(active,selected,foreground,legacy,Forge.isMobileAdventureMode);}
    public static Color buttonText(boolean enabled,boolean selected){
        ResolvedTheme theme=presentationForAdventure();
        return buttonText(theme,enabled,selected,false);
    }
    public static Color buttonText(ResolvedTheme theme,boolean enabled,boolean selected,boolean hover){
        return theme==null?Color.WHITE:Color.valueOf(theme.color(!enabled?"state.onDisabled":selected?"action.onPrimary":hover?"text.primary":"action.onSecondary"));
    }
    public static Color rowText(ResolvedTheme theme,boolean selected,Color legacy,boolean adventure){
        return adventure&&theme!=null&&selected?Color.valueOf(theme.color("state.onSelected")):legacy;
    }
    public static void drawButton(Graphics g,float width,float height,boolean hover,boolean pressed,boolean enabled,boolean selected){
        ResolvedTheme theme=presentationForAdventure();if(theme==null)return;
        Color fill=Color.valueOf(theme.color(!enabled?"state.disabled":pressed||selected?"action.primary":hover?"state.hover":"action.secondary"));
        Color frame=Color.valueOf(theme.color(hover?"state.focus":"border.frame"));
        float radius=theme.shape().equals("angular")?0:Utils.scale(theme.shape().equals("leaf")?5:3);
        g.fillRoundRect(fill,0,0,width,height,radius);
        g.drawRect(Math.max(1,Utils.scale(.75f)),frame,.5f,.5f,Math.max(0,width-1),Math.max(0,height-1));
        if(!theme.ornament().equals("quiet")){
            Color light=Color.valueOf(theme.color("border.highlight"));float inset=Utils.scale(3);
            g.drawLine(Math.max(1,Utils.scale(.35f)),light,inset,inset,Math.max(inset,width-inset),inset);
            if(theme.ornament().equals("full"))g.fillCircle(frame,width-inset,height-inset,Utils.scale(.8f));
        }
    }
}
