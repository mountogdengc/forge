package forge.adventure.editor;

import forge.adventure.util.Config;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class EditorMainWindow extends JFrame {
    public static WorldEditor worldEditor;
    JTabbedPane tabs = new JTabbedPane();

    public EditorMainWindow(Config config) {
        UIManager.LookAndFeelInfo[] var1 = UIManager.getInstalledLookAndFeels();

        for (UIManager.LookAndFeelInfo info : var1) {
            if ("Nimbus".equals(info.getName())) {
                try {
                    UIManager.setLookAndFeel(info.getClassName());
                } catch (Throwable ignored) {
                }
                break;
            }
        }
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setTitle("Campaign Editor - " + (config.getCampaign() == null ? config.getPlane() : config.getCampaign().getTitle()));
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setVisible(false);
                System.exit(0);
            }
        });
        BorderLayout layout = new BorderLayout();
        JToolBar toolBar = new JToolBar("toolbar");
        JButton newButton = new JButton("GDX Particle Editor Tool");
        newButton.addActionListener(e -> EventQueue.invokeLater(() -> {
            newButton.setEnabled(false);
            try {
                CodeSource codeSource = EditorMainWindow.class.getProtectionDomain().getCodeSource();
                File jarFile = new File(codeSource.getLocation().toURI().getPath());
                String jarDir = jarFile.getParentFile().getPath();
                Desktop.getDesktop().open(new File(jarDir + "/gdx-particle-editor.jar"));
            } catch (Exception ex) {
                new ErrorDialog("Error", ex.getMessage());
                newButton.setEnabled(true);
            }
        }));
        JButton quit = new JButton("Quit");
        quit.addActionListener(e -> System.exit(0));
        toolBar.add(newButton);
        toolBar.add(quit);
        JButton check = new JButton("Validate Campaign");
        javax.swing.JLabel checkStatus = new javax.swing.JLabel("Campaign checks pending");
        toolBar.add(check); toolBar.addSeparator(); toolBar.add(checkStatus);
        setLayout(layout);
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(new javax.swing.JLabel("Campaign: " + config.getPrefix() + "   |   Restart the game after saving campaign edits."), BorderLayout.SOUTH);
        worldEditor = null;
        WorldEditor.atlas.clear();
        QuestController.reset();
        final CampaignOverviewPanel[] overview = new CampaignOverviewPanel[1];
        overview[0] = new CampaignOverviewPanel(config, name -> {
            int index = tabs.indexOfTab(name);
            if (index >= 0) tabs.setSelectedIndex(index);
        }, () -> setTitle("Campaign Editor - " + overview[0].campaignTitle()));
        overview[0].setCheckStatus(checkStatus::setText);
        check.addActionListener(e -> { tabs.setSelectedIndex(0); overview[0].validateCampaign(); });
        tabs.addTab("Overview", overview[0]);
        addTool("World", () -> { worldEditor = new WorldEditor(); return worldEditor; });
        addTool("Locations", PointOfInterestEditor::new);
        addTool("Items", ItemsEditor::new);
        addTool("Enemies", EnemyEditor::new);
        addTool("Quests", QuestEditor::new);
        addTool("Allowed sets", () -> new AllowedSetsEditor(config));
        addTool("Files", () -> new CampaignFilesEditor(config, () -> { overview[0].refresh(); setTitle("Campaign Editor - " + overview[0].campaignTitle()); overview[0].validateCampaign(); }));
        java.util.Map<java.awt.Component, Boolean> enabledBeforeCopy = new java.util.IdentityHashMap<>();
        overview[0].setMapLifecycle(() -> {
            LazyTool tool = (LazyTool) tabs.getComponentAt(tabs.indexOfTab("Locations"));
            if (tool.loaded && tool.getComponent(0) instanceof PointOfInterestEditor locations) locations.save();
            disableForCopy(getContentPane(), enabledBeforeCopy);
        }, () -> {
            LazyTool tool = (LazyTool) tabs.getComponentAt(tabs.indexOfTab("Locations"));
            try { if (tool.loaded && tool.getComponent(0) instanceof PointOfInterestEditor locations) locations.load(false); }
            finally { enabledBeforeCopy.forEach(java.awt.Component::setEnabled); enabledBeforeCopy.clear(); }
        });
        tabs.addChangeListener(e -> {
            java.awt.Component selected = tabs.getSelectedComponent();
            if (selected instanceof LazyTool tool) tool.load();
            if (selected == overview[0]) overview[0].refresh();
        });
        setSize(config.getSettingData().width, config.getSettingData().height);
        setLocationRelativeTo(null);
        setVisible(true);
        EventQueue.invokeLater(overview[0]::validateCampaign);
    }

    private static void disableForCopy(java.awt.Component component, java.util.Map<java.awt.Component, Boolean> states) {
        states.put(component, component.isEnabled()); component.setEnabled(false);
        if (component instanceof java.awt.Container container)
            for (java.awt.Component child : container.getComponents()) disableForCopy(child, states);
    }
    private void addTool(String title, java.util.function.Supplier<javax.swing.JComponent> factory) {
        tabs.addTab(title, new LazyTool(factory));
    }
    static final class LazyTool extends javax.swing.JPanel {
        private final java.util.function.Supplier<javax.swing.JComponent> factory;
        private boolean loaded;
        LazyTool(java.util.function.Supplier<javax.swing.JComponent> factory) {
            super(new BorderLayout()); this.factory = factory;
        }
        void load() {
            if (loaded) return;
            removeAll();
            try { add(factory.get(), BorderLayout.CENTER); loaded = true; }
            catch (RuntimeException error) {
                javax.swing.JTextArea message = new javax.swing.JTextArea("This tool could not read the campaign files. Use Overview > Validate Campaign for details.\n\n" + error.getMessage());
                message.setEditable(false); message.setLineWrap(true); message.setWrapStyleWord(true);
                add(new javax.swing.JScrollPane(message), BorderLayout.CENTER);
                JButton retry = new JButton("Retry after repairing files"); retry.addActionListener(e -> load()); add(retry, BorderLayout.SOUTH);
            }
            revalidate(); repaint();
        }
    }

    static class ErrorDialog {
        public ErrorDialog(String title, String message) {
            List<Object> options = new ArrayList<>();
            JButton ok = new JButton("OK");
            options.add(ok);
            JOptionPane pane = new JOptionPane(message, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, options.toArray());
            JDialog dlg = pane.createDialog(JOptionPane.getRootFrame(), title);
            ok.addActionListener(e -> {
                dlg.setVisible(false);
                System.exit(0);
            });
            dlg.setResizable(false);
            dlg.setVisible(true);
        }
    }
}
