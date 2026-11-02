package forge.adventure;

import forge.GuiMobile;
import forge.adventure.editor.EditorMainWindow;
import forge.adventure.util.Config;
import forge.gui.GuiBase;
import forge.adventure.campaign.CampaignDefinition;
import javax.swing.JOptionPane;
import java.awt.EventQueue;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Main entry point
 */
public class Main {

    public static void main(String[] args) {
        initializeEnvironment();
        EventQueue.invokeLater(() -> {
            openCampaignPicker(args);
        });
    }

    public static void initializeEnvironment() {
        GuiBase.setInterface(new GuiMobile(Files.exists(Paths.get("./res"))?"./":"../forge-gui/"));
        GuiBase.setDeviceInfo(null, 0, System.getProperty("user.home") + "/Downloads/");
        String language = new forge.localinstance.properties.ForgePreferences().getPref(
                forge.localinstance.properties.ForgePreferences.FPref.UI_LANGUAGE);
        forge.util.Localizer.getInstance().initialize(language, forge.localinstance.properties.ForgeConstants.LANG_DIR);
        forge.util.Lang.createInstance(language);
    }

    private static void openCampaignPicker(String[] args) {
            try {
                String requested = args.length == 2 && args[0].equals("--campaign") ? args[1] : null;
                CampaignDefinition selected = forge.adventure.editor.CampaignChooserDialog.choose(requested);
                if (selected == null) return;
                Config.activateCampaign(Config.forEditing(selected));
                new EditorMainWindow(Config.instance());
            } catch (Exception error) {
                JOptionPane.showMessageDialog(null, "Could not open Campaign Editor: " + error.getMessage(),
                        "Campaign Editor", JOptionPane.ERROR_MESSAGE);
            }
    }
}
