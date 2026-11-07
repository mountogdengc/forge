package forge.adventure.theme;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ThemeSkinAdapterTest {
    @Test public void secondaryChooserAndHoverUseTheValidatedForegroundPairs() throws Exception {
        var root=ThemeResolverTest.roots();ThemeResolverTest.manifest(root.resolve("users"),"light",ThemeResolverTest.child("user.light","folio",",\"colors\":{\"action.secondary\":\"#ffffff\",\"action.onSecondary\":\"#000000\"}"));
        var theme=ThemeResolver.resolve(ThemeResolverTest.catalog(root),"user.light");Skin legacy=legacy();
        com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle list=new com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle();list.font=legacy.getFont("default");
        legacy.add("default",list);ScrollPane.ScrollPaneStyle scroll=new ScrollPane.ScrollPaneStyle();legacy.add("default",scroll);
        SelectBox.SelectBoxStyle select=new SelectBox.SelectBoxStyle();select.scrollStyle=scroll;select.listStyle=list;select.font=legacy.getFont("default");legacy.add("default",select);
        Skin adapted=ThemeSkinAdapter.adapt(legacy,theme);
        assertEquals(adapted.get(SelectBox.SelectBoxStyle.class).fontColor,Color.BLACK);
        assertEquals(adapted.get(SelectBox.SelectBoxStyle.class).overFontColor,Color.BLACK,
                "An open or hovered chooser must keep its secondary foreground pair");
        assertEquals(ThemePaletteBridge.buttonText(theme,true,false,true),Color.valueOf("f1e6ce"));
    }
    @Test public void limitedCustomBodyFontPreservesTheLegacyLocalizedGlyphs(){
        BitmapFont reference=legacy().getFont("default"),limited=legacy().getFont("default");
        BitmapFont.Glyph omega=new BitmapFont.Glyph();omega.id=0x03a9;omega.width=1;omega.height=1;
        reference.getData().setGlyph(0x03a9,omega);
        assertSame(ThemeSkinAdapter.compatibleBodyFont(limited,reference),reference);
        assertSame(ThemeSkinAdapter.compatibleBodyFont(reference,reference),reference);
    }
    @Test public void standardTabsConsumeTheirDeclaredStyle() throws Exception {
        Skin themed=ThemeSkinAdapter.adapt(legacy(),theme());
        assertTrue(themed.has("theme.tab",TextButton.TextButtonStyle.class));
        assertNotSame(themed.get("theme.tab",TextButton.TextButtonStyle.class).up,themed.get(TextButton.TextButtonStyle.class).up);
    }
    static Skin legacy() {
        Skin skin=new Skin();
        BitmapFont.BitmapFontData data=new BitmapFont.BitmapFontData();
        data.lineHeight=12;data.capHeight=9;data.xHeight=7;data.spaceXadvance=5;
        BitmapFont font=new BitmapFont(data,new Array<>(new TextureRegion[]{new TextureRegion()}),false);
        skin.add("default",font,BitmapFont.class);
        skin.add("white-pixel",new TextureRegion(),TextureRegion.class);
        BaseDrawable background=new BaseDrawable();background.setMinHeight(22);background.setLeftWidth(6);
        Label.LabelStyle label=new Label.LabelStyle(font,Color.WHITE);
        TextButton.TextButtonStyle button=new TextButton.TextButtonStyle();button.font=font;button.fontColor=Color.WHITE;button.up=background;
        skin.add("default",label,Label.LabelStyle.class);skin.add("default",button,TextButton.TextButtonStyle.class);
        skin.add("custom-story",new Label.LabelStyle(font,Color.PINK),Label.LabelStyle.class);
        skin.add("unique-emblem",background,Drawable.class);
        return skin;
    }
    static ResolvedTheme theme() throws Exception {
        return ThemeResolver.resolve(ThemeResolverTest.catalog(ThemeResolverTest.roots()),"folio");
    }
    @Test public void adaptingCannotMutateLegacyStyleOrFont() throws Exception {
        Skin legacy=legacy();Skin themed=ThemeSkinAdapter.adapt(legacy,theme());
        assertNotSame(themed,legacy);
        assertNotSame(themed.get(Label.LabelStyle.class),legacy.get(Label.LabelStyle.class));
        assertEquals(legacy.get(Label.LabelStyle.class).fontColor,Color.WHITE);
        assertEquals(themed.get(Label.LabelStyle.class).fontColor,Color.valueOf("f1e6ce"));
        themed.getFont("default").getData().setScale(2);
        assertEquals(legacy.getFont("default").getData().scaleX,1f);
        assertEquals(themed.get(TextButton.TextButtonStyle.class).up.getMinHeight(),22f);
        assertEquals(themed.get(TextButton.TextButtonStyle.class).up.getLeftWidth(),6f);
    }
    @Test public void customStyleNamesRemainAvailable() throws Exception {
        Skin legacy=legacy();Skin themed=ThemeSkinAdapter.adapt(legacy,theme());
        assertSame(themed.get("custom-story",Label.LabelStyle.class),legacy.get("custom-story",Label.LabelStyle.class));
        assertSame(themed.getDrawable("unique-emblem"),legacy.getDrawable("unique-emblem"));
        assertEquals(legacy.get("custom-story",Label.LabelStyle.class).fontColor,Color.PINK);
    }
    @Test public void missingOptionalSlotsUseFolio() throws Exception {
        Skin themed=ThemeSkinAdapter.adapt(legacy(),theme());
        assertNotNull(themed.getDrawable("theme.paper"));
        assertNotNull(themed.getDrawable("theme.panel"));
        assertTrue(themed.get(TextButton.TextButtonStyle.class).up instanceof ThemeDrawable);
        assertTrue(themed.getDrawable("theme.paper") instanceof com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable,
                "Textra backgrounds require the transform drawable contract");
    }
    @Test public void missingRequiredLegacyContractReturnsLegacySkinAndNotice() throws Exception {
        Skin custom=new Skin();assertSame(ThemeSkinAdapter.adapt(custom,theme()),custom);
        assertFalse(ThemeSkinAdapter.compatibilityIssue(custom).isBlank());
    }
}
