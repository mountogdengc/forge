package forge.adventure.editor;

import forge.adventure.campaign.CampaignDefinition;
import forge.adventure.campaign.CampaignRegistry;
import forge.adventure.util.Config;
import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises actual wizard worker and overview in invisible, unfocused Swing peers. */
public final class CampaignAuthoringSmoke {
    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    public static void main(String[] args) throws Exception {
        if (!Boolean.getBoolean("editor.isolatedProfile")) throw new IllegalStateException("Disposable profile required");
        forge.adventure.Main.initializeEnvironment();
        Path installed = Path.of(Config.instance().getAdventureRoot());
        Path projects = Path.of(forge.localinstance.properties.ForgeConstants.USER_ADVENTURE_DIR, "userplanes");
        var errors = new ArrayList<String>();
        var campaigns = CampaignChooserDialog.discover(installed, projects, errors);
        var templates = campaigns.stream().filter(NewCampaignWizard::isTemplate).toList();
        if (templates.stream().noneMatch(c -> c.getId().equals("fork.bellwake"))) throw new AssertionError("Bellwake template missing");
        AtomicReference<NewCampaignWizard> wizard = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                NewCampaignWizard w = new NewCampaignWizard(null, installed.resolve("common"), projects, templates);
                wizard.set(w);
                if (!((CampaignDefinition)((JComboBox<?>)field(w,"template")).getSelectedItem()).getId().equals("fork.bellwake")) throw new AssertionError("Wrong default template");
                ((JTextField)field(w,"title")).setText("Authoring Test");
                ((JTextArea)field(w,"description")).setText("Private map test");
                ((JButton)field(w,"create")).doClick();
                if (((JButton)field(w,"cancel")).isEnabled()) throw new AssertionError("Cancel enabled during creation");
            } catch (Exception error) { throw new RuntimeException(error); }
        });
        AtomicReference<CampaignDefinition> created = new AtomicReference<>();
        long deadline = System.nanoTime() + 45_000_000_000L;
        while (created.get() == null && System.nanoTime() < deadline) {
            SwingUtilities.invokeAndWait(() -> { try { created.set((CampaignDefinition)field(wizard.get(),"created")); } catch (Exception e) { throw new RuntimeException(e); } });
            Thread.sleep(100);
        }
        if (created.get() == null) throw new AssertionError("Wizard did not create campaign");
        var project = created.get();
        if (!CampaignRegistry.discover(installed, projects, false).findById(project.getId()).getRoot().equals(project.getRoot())) throw new AssertionError("Game cannot discover new project");
        Config.activateCampaign(Config.forEditing(project));
        AtomicReference<EditorMainWindow> frame = new AtomicReference<>();
        AtomicReference<CampaignOverviewPanel> overview = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            EditorMainWindow w = new EditorMainWindow(Config.instance()) { @Override public void setVisible(boolean visible) { super.setVisible(false); } };
            frame.set(w); CampaignOverviewPanel panel = (CampaignOverviewPanel)w.tabs.getComponentAt(0); overview.set(panel);
            if (EditorMainWindow.worldEditor != null) throw new AssertionError("Eager world tool");
            if (panel.locations.isEmpty() || !panel.locations.get(0).map().startsWith("campaign:")) throw new AssertionError("Missing private maps");
            panel.validate.doClick();
        });
        AtomicReference<String> result = new AtomicReference<>(); deadline = System.nanoTime() + 45_000_000_000L;
        while (result.get() == null && System.nanoTime() < deadline) {
            SwingUtilities.invokeAndWait(() -> { if (overview.get().validate.isEnabled()) result.set(overview.get().results.getText()); });
            Thread.sleep(100);
        }
        if (result.get() == null || !result.get().startsWith("Validation passed")) throw new AssertionError("Overview validation: " + result.get());
        SwingUtilities.invokeAndWait(() -> {
            for (Window window : Window.getWindows()) if (window.isVisible() || window.isFocused()) throw new AssertionError("Visible/focused authoring window");
            frame.get().dispose(); wizard.get().dispose();
        });
        Files.writeString(Path.of(System.getProperty("editor.projectEvidence")), project.getRoot() + "\n" + project.getId() + "\n");
        System.out.println("CAMPAIGN_AUTHORING_PASS: actual wizard worker, private maps, game discovery, lazy overview, real card validation; invisible/unfocused");
        System.exit(0);
    }
}
