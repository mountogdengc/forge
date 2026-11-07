package forge.adventure.theme;

import com.badlogic.gdx.graphics.Color;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ThemePaletteBridgeTest {
    @Test public void selectedDeckRowsUseTheirPairedForeground() throws Exception {
        ResolvedTheme theme=ThemeSkinAdapterTest.theme();
        assertEquals(ThemePaletteBridge.rowText(theme,true,Color.WHITE,true),Color.valueOf("102820"));
        assertSame(ThemePaletteBridge.rowText(theme,true,Color.WHITE,false),Color.WHITE);
    }
    @Test public void adventureRolesMapToTheResolvedTheme() throws Exception {
        ResolvedTheme theme=ThemeSkinAdapterTest.theme();Color legacy=Color.MAGENTA;
        assertEquals(ThemePaletteBridge.colorFor(theme,"ADV_CLR_TEXT",legacy,true),Color.valueOf("f1e6ce"));
        assertEquals(ThemePaletteBridge.colorFor(theme,"ADV_CLR_THEME",legacy,true),Color.valueOf("102820"));
        assertEquals(ThemePaletteBridge.colorFor(theme,"ADV_CLR_COMBAT_TARGETING_ARROW",legacy,true),Color.valueOf("ef9b91"));
    }
    @Test public void classicRolesAndLegacyModeRemainUnchanged() throws Exception {
        ResolvedTheme theme=ThemeSkinAdapterTest.theme();Color old=new Color(.2f,.4f,.6f,1);
        assertSame(ThemePaletteBridge.colorFor(theme,"CLR_TEXT",old,true),old);
        assertSame(ThemePaletteBridge.colorFor(theme,"ADV_CLR_TEXT",old,false),old);
        assertSame(ThemePaletteBridge.colorFor(null,"ADV_CLR_TEXT",old,true),old);
    }
    @Test public void phaseStatesRemainDistinctAndReadable(){
        ThemeCatalog catalog=ThemeCatalog.snapshot(BundledThemeTest.builtins(),null);
        for(String id:new String[]{"folio","midnight","verdant"}){
            ResolvedTheme theme=ThemeResolver.resolve(catalog,id);java.util.Set<String> fills=new java.util.HashSet<>();
            for(boolean active:new boolean[]{false,true})for(boolean stop:new boolean[]{false,true}){
                Color fill=ThemePaletteBridge.phaseFill(theme,active,stop),text=ThemePaletteBridge.phaseText(theme,active,stop);
                fills.add(fill.toString());
                assertTrue(ThemeResolver.contrast("#"+fill.toString().substring(0,6),"#"+text.toString().substring(0,6))>=4.5);
            }
            assertEquals(fills.size(),4);
        }
    }
    @Test public void progressBarsUseActiveThemeInsteadOfStaleSpritePixels() throws Exception {
        ResolvedTheme theme=ThemeSkinAdapterTest.theme();
        assertEquals(ThemePaletteBridge.progressColor(theme,false,false,Color.MAGENTA,true),Color.valueOf("203630"));
        assertEquals(ThemePaletteBridge.progressColor(theme,true,false,Color.MAGENTA,true),Color.valueOf("70bca5"));
        assertEquals(ThemePaletteBridge.progressColor(theme,true,true,Color.MAGENTA,true),Color.valueOf("102820"));
    }
    @Test public void enterExitAdventureDoesNotMutateClassicPalette() throws Exception {
        Color classic=new Color(.15f,.25f,.35f,.6f);ResolvedTheme theme=ThemeSkinAdapterTest.theme();
        ThemePaletteBridge.colorFor(theme,"ADV_CLR_TEXT",classic,true);
        assertSame(ThemePaletteBridge.colorFor(theme,"CLR_TEXT",classic,false),classic);
        assertEquals(classic,new Color(.15f,.25f,.35f,.6f));
    }
}
