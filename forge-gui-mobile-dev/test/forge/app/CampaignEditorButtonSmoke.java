package forge.app;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.github.tommyettinger.textra.TextraButton;
import forge.Forge;
import forge.adventure.campaign.CampaignManager;
import forge.adventure.campaign.CampaignRegistry;
import forge.adventure.scene.StartScene;
import forge.adventure.scene.UIScene;
import forge.adventure.util.AdventureModes;
import forge.adventure.util.Config;
import forge.adventure.util.UIActor;
import forge.adventure.world.WorldSave;
import forge.card.ColorSet;

/** Exercises the real menu; process launching is checked separately with a real headless child. */
final class CampaignEditorButtonSmoke {
    static int launches;
    static String requested;
    static void run() throws Exception {
        var scene = StartScene.instance();
        var field = UIScene.class.getDeclaredField("ui"); field.setAccessible(true);
        UIActor ui = (UIActor) field.get(scene);
        TextraButton editor = ui.findActor("CampaignEditor"), save = ui.findActor("Save");
        require(editor != null && editor.isVisible() && !save.isVisible(), "Startup editor button missing");
        Rectangle bounds = new Rectangle(editor.getX(), editor.getY(), editor.getWidth(), editor.getHeight());
        require(bounds.x >= 0 && bounds.y >= 0 && bounds.x+bounds.width <= ui.getWidth()
                && bounds.y+bounds.height <= ui.getHeight(), "Editor button clipped");
        for (String name : new String[]{"Start","Start+","Load","Continue","Settings","Backup","Exit","Switch"}) {
            TextraButton other = ui.findActor(name);
            if (other != null && other.isVisible()) require(!bounds.overlaps(new Rectangle(other.getX(),other.getY(),other.getWidth(),other.getHeight())), "Editor overlaps " + name);
        }
        String campaign = CampaignManager.current().getId();
        boolean canContinue = WorldSave.canContinue();
        for (var listener : new com.badlogic.gdx.utils.Array<>(editor.getListeners()))
            if (listener instanceof ClickListener click) click.clicked(null,0,0);
        require(launches == 1 && campaign.equals(requested), "Editor button did not call desktop launcher");
        require(!WorldSave.hasActivePlaythrough() && canContinue == WorldSave.canContinue()
                && campaign.equals(CampaignManager.current().getId()), "Opening editor changed game state");
        Pixmap shot = Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
        try { PixmapIO.writePNG(Gdx.files.absolute(System.getProperty("bellwake.screenshot")),shot,6,true); }
        finally { shot.dispose(); }
        var selected = CampaignRegistry.discover().findById("fork.curse-of-strahd");
        CampaignManager.activate(selected);
        WorldSave.beginPlaythrough(selected,"Editor button smoke","Editor Smoke");
        WorldSave.generateNewWorld("Editor Smoke",true,0,0,ColorSet.fromNames("W"),
                Config.instance().getConfigData().difficulties[0],AdventureModes.Constructed,0,null,0);
        Forge.switchScene(StartScene.instance());
        ui = (UIActor) field.get(StartScene.instance());
        require(!((TextraButton) ui.findActor("CampaignEditor")).isVisible()
                && ((TextraButton) ui.findActor("Save")).isVisible(), "Editor replaced Save during active play");
        System.out.println("CAMPAIGN_EDITOR_BUTTON_PASS: visible startup button, clear bounds, callback, unchanged startup state, hidden during active play");
    }
    private static void require(boolean ok,String message) { if (!ok) throw new AssertionError(message); }
}
