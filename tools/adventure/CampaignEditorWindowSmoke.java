package forge.adventure.editor;

import forge.adventure.campaign.CampaignRegistry;
import forge.adventure.util.Config;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Initializes genuine editor forms in an invisible Swing peer; never requests focus. */
public final class CampaignEditorWindowSmoke {
    private static javax.swing.JButton findButton(java.awt.Container root, String text) {
        for (java.awt.Component component : root.getComponents()) {
            if (component instanceof javax.swing.JButton button && button.getText().equals(text)) return button;
            if (component instanceof java.awt.Container container) {
                javax.swing.JButton button = findButton(container, text);
                if (button != null) return button;
            }
        }
        return null;
    }
    public static void main(String[] args) throws Exception {
        if (!Boolean.getBoolean("editor.isolatedProfile")) throw new IllegalStateException("Disposable profile required");
        forge.adventure.Main.initializeEnvironment();
        var campaign = CampaignRegistry.discover().findById("fork.curse-of-strahd");
        if (campaign == null) throw new AssertionError("Strahd campaign missing");
        Config.activateCampaign(Config.forCampaign(campaign));
        SwingUtilities.invokeAndWait(() -> {
            EditorMainWindow frame = new EditorMainWindow(Config.instance()) {
                @Override public void setVisible(boolean visible) { super.setVisible(false); }
            };
            try {
                if (!frame.getTitle().contains("Curse of Strahd")) throw new AssertionError("Missing campaign title");
                if (frame.tabs.getTabCount() != 8 || !frame.tabs.getTitleAt(6).equals("Allowed sets")) throw new AssertionError("Missing editor tools");
                javax.swing.JButton copyMaps = findButton(frame, "Create Editable Map Copies");
                if (copyMaps == null || copyMaps.getToolTipText() == null
                        || !copyMaps.getToolTipText().contains("without affecting other campaigns")
                        || !copyMaps.getToolTipText().contains("campaign identity stay the same"))
                    throw new AssertionError("Map-copy label or explanation missing");
                if (EditorMainWindow.worldEditor != null) throw new AssertionError("World loaded before its tab was selected");
                for (int i = 1; i < frame.tabs.getTabCount(); i++) {
                    frame.tabs.setSelectedIndex(i);
                    if (!((EditorMainWindow.LazyTool)frame.tabs.getComponentAt(i)).getComponent(0).getClass().getSimpleName().endsWith("Editor"))
                        throw new AssertionError("Tool did not load: " + frame.tabs.getTitleAt(i));
                }
                frame.tabs.setSelectedIndex(0);
                if (EditorMainWindow.worldEditor.list.getModel().getSize() > 0) EditorMainWindow.worldEditor.list.setSelectedIndex(0);
                frame.setSize(1280,800);
                frame.addNotify(); frame.validate();
                for (Window window : Window.getWindows())
                    if (window.isVisible() || window.isFocused()) throw new AssertionError("Editor smoke showed a window");
                BufferedImage image = new BufferedImage(1280,800,BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();
                frame.getRootPane().printAll(graphics); graphics.dispose();
                try { ImageIO.write(image,"png",Path.of(System.getProperty("editor.screenshot")).toFile()); }
                catch (java.io.IOException error) { throw new RuntimeException(error); }
                System.out.println("CAMPAIGN_EDITOR_WINDOW_PASS: overview and seven lazy tools, selected campaign, invisible/unfocused Swing window");
                System.out.println("CAMPAIGN_EDITOR_HEAP_MB=" + java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()/1048576);
            } finally { frame.dispose(); }
        });
        System.exit(0);
    }
}
