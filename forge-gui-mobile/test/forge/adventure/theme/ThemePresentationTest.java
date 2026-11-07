package forge.adventure.theme;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ThemePresentationTest {
    @Test public void neutralPaperMarkupUsesItsRoleAndPreservesWarningColors() throws Exception {
        var root=ThemeResolverTest.roots();
        ThemeResolverTest.manifest(root.resolve("users"),"dark-paper",ThemeResolverTest.child("user.dark-paper","folio",",\"colors\":{\"surface.paper\":\"#101010\",\"text.onPaper\":\"#eeeeee\",\"phase.activeDisabled\":\"#101010\"}"));
        ResolvedTheme theme=ThemeResolver.resolve(ThemeResolverTest.catalog(root),"user.dark-paper");
        assertEquals(ThemePresentation.paperMarkup("[BLACK]Journal[RED]Warning",theme,"text.onPaper"),"[#eeeeee]Journal[RED]Warning");
        assertEquals(ThemePresentation.paperMarkup("[BLACK]Custom",theme,"text.primary"),"[BLACK]Custom");
    }
    @Test public void labelsWithTheirOwnPaperBackingKeepThePairedInk() throws Exception {
        ResolvedTheme theme=ThemeSkinAdapterTest.theme();Skin skin=ThemeSkinAdapter.adapt(ThemeSkinAdapterTest.legacy(),theme);
        Label label=new Label("Resource count",skin);Label.LabelStyle style=new Label.LabelStyle(label.getStyle());
        style.background=skin.getDrawable("theme.paper");label.setStyle(style);
        ThemePresentation.styleTree(label,theme,skin,false);
        assertEquals(label.getStyle().fontColor,Color.valueOf(theme.color("text.onPaper")));
    }
    @Test public void resourceBackingAndGlassHaveActualPresentationConsumers() throws Exception {
        Skin skin=ThemeSkinAdapter.adapt(ThemeSkinAdapterTest.legacy(),ThemeSkinAdapterTest.theme());
        assertSame(ThemePresentation.chromeFor(skin),skin.getDrawable("theme.glass"));
        float[] drawn=new float[2];
        BaseDrawable hud=new BaseDrawable(){@Override public void draw(com.badlogic.gdx.graphics.g2d.Batch batch,float x,float y,float w,float h){drawn[0]=w;}};
        BaseDrawable resources=new BaseDrawable(){@Override public void draw(com.badlogic.gdx.graphics.g2d.Batch batch,float x,float y,float w,float h){drawn[1]=w;}};
        ThemePresentation.hudBacking(hud,resources).draw(null,0,0,75,104);
        assertEquals(drawn[0],75f);assertEquals(drawn[1],71f);
    }
    @Test public void presentationCannotReplaceMapActorCardOrStoryArt() throws Exception {
        Skin skin=ThemeSkinAdapter.adapt(ThemeSkinAdapterTest.legacy(),ThemeSkinAdapterTest.theme());
        assertTrue(ThemePresentation.drawableFor("hud.json","map","ui/minimap.png",skin).isEmpty());
        assertTrue(ThemePresentation.drawableFor("hud.json","card","custom_card_pics/card.png",skin).isEmpty());
        assertTrue(ThemePresentation.drawableFor("story.json","background","ui/title_bg.png",skin).isEmpty());
        assertTrue(ThemePresentation.drawableFor("hud.json","mapborder","ui/minimap.png",skin).isPresent());
    }
    @Test public void actorNamesBoundsAndSelectionOrderPreserved() throws Exception {
        Image actor=new Image(new BaseDrawable());actor.setName("mapborder");actor.setBounds(13,17,80,80);
        ThemePresentation.style(actor,"hud.json",actor.getName(),ThemeSkinAdapterTest.theme());
        assertEquals(actor.getName(),"mapborder");assertEquals(actor.getX(),13f);assertEquals(actor.getY(),17f);
        assertEquals(actor.getWidth(),80f);assertEquals(actor.getHeight(),80f);
    }
    @Test public void legacySwatchDoesNotRequireThemedAssets(){
        assertNotNull(ThemePresentation.swatchDrawable(ThemeSkinAdapterTest.legacy()));
    }
    @Test public void bespokeCampaignRetainsUnknownDrawablesAndLayout() throws Exception {
        Skin custom=ThemeSkinAdapterTest.legacy();Skin themed=ThemeSkinAdapter.adapt(custom,ThemeSkinAdapterTest.theme());
        assertTrue(ThemePresentation.drawableFor("hud.json","mapborder","ui/bespoke-compass.png",themed).isEmpty());
        assertTrue(ThemePresentation.drawableFor("custom.json","mapborder","ui/minimap.png",themed).isEmpty());
        assertSame(themed.getDrawable("unique-emblem"),custom.getDrawable("unique-emblem"));
    }
    @Test public void paperPassPreservesCustomStylesAndMeaningfulStateColors() throws Exception {
        ResolvedTheme theme=ThemeSkinAdapterTest.theme();
        Skin skin=ThemeSkinAdapter.adapt(ThemeSkinAdapterTest.legacy(),theme);
        Label ordinary=new Label("body",skin),custom=new Label("story",skin,"custom-story"),warning=new Label("warning",skin);
        warning.setColor(Color.RED);Group root=new Group();root.addActor(ordinary);root.addActor(custom);root.addActor(warning);
        ThemePresentation.styleTree(root,theme,skin,true);
        assertEquals(ordinary.getStyle().fontColor,Color.valueOf("302b22"));
        assertEquals(custom.getStyle().fontColor,Color.PINK);
        assertEquals(warning.getColor(),Color.RED);
    }
}
