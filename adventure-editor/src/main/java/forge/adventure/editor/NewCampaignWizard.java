package forge.adventure.editor;

import com.badlogic.gdx.utils.JsonReader;
import forge.adventure.campaign.CampaignDefinition;
import forge.adventure.campaign.CampaignRegistry;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;

/** Creates a private campaign copy in a worker while keeping the wizard responsive. */
public final class NewCampaignWizard extends JDialog {
    private final Path commonRoot, projectsRoot;
    private final JTextField title = new JTextField(36);
    private final JTextArea description = new JTextArea(4, 36);
    private final JComboBox<CampaignDefinition> template;
    private final JTextArea destination = new JTextArea(3, 36);
    private final JLabel status = new JLabel(" ");
    private final JButton create = new JButton("Create Campaign"), cancel = new JButton("Cancel");
    private boolean busy;
    private CampaignDefinition created;

    static CampaignDefinition create(Window owner, Path commonRoot, Path projectsRoot, List<CampaignDefinition> templates) {
        NewCampaignWizard wizard = new NewCampaignWizard(owner, commonRoot, projectsRoot, templates);
        wizard.setVisible(true);
        return wizard.created;
    }

    NewCampaignWizard(Window owner, Path commonRoot, Path projectsRoot, List<CampaignDefinition> templates) {
        super(owner, "New Campaign", ModalityType.APPLICATION_MODAL);
        this.commonRoot = commonRoot.toAbsolutePath().normalize();
        this.projectsRoot = projectsRoot.toAbsolutePath().normalize();
        template = new JComboBox<>(templates.toArray(new CampaignDefinition[0]));
        template.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof CampaignDefinition campaign) label.setText(campaign.getTitle() + " (" + campaign.getVersion() + ") — " + campaign.getRoot().getFileName());
                return label;
            }
        });
        for (CampaignDefinition candidate : templates) if (candidate.getId().equals("fork.bellwake")) template.setSelectedItem(candidate);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent event) { if (!busy) dispose(); } });
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setBorder(BorderFactory.createEmptyBorder(14, 14, 8, 14));
        description.setLineWrap(true); description.setWrapStyleWord(true);
        destination.setEditable(false); destination.setLineWrap(true); destination.setWrapStyleWord(true);
        destination.setBackground(fields.getBackground());
        row(fields, 0, "Title", title);
        row(fields, 1, "Description", new JScrollPane(description));
        row(fields, 2, "Template", template);
        row(fields, 3, "Destination", destination);
        JTextArea explanation = new JTextArea("The template's story, encounters, opening quests, decks and allowed sets are copied. Location maps and their dependencies become private editable files. You can change allowed sets in Overview after creation.", 4, 36);
        explanation.setEditable(false); explanation.setLineWrap(true); explanation.setWrapStyleWord(true); explanation.setBackground(fields.getBackground());
        row(fields, 4, "", explanation);
        add(fields, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(8, 8));
        footer.setBorder(BorderFactory.createEmptyBorder(0, 14, 14, 14));
        footer.add(status, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); buttons.add(create); buttons.add(cancel);
        footer.add(buttons, BorderLayout.SOUTH); add(footer, BorderLayout.SOUTH);
        title.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { updateDestination(); }
            public void removeUpdate(DocumentEvent event) { updateDestination(); }
            public void changedUpdate(DocumentEvent event) { updateDestination(); }
        });
        create.addActionListener(event -> startCreation()); cancel.addActionListener(event -> { if (!busy) dispose(); });
        getRootPane().setDefaultButton(create);
        updateDestination(); pack(); setMinimumSize(new Dimension(650, 520)); setLocationRelativeTo(owner);
    }

    static boolean isTemplate(CampaignDefinition campaign) {
        if (campaign.isLegacy()) return false;
        Path root = campaign.getRoot();
        for (String file : new String[]{"campaign.json", "config.json", "world/world.json", "world/quests.json",
                "world/enemies.json", "world/points_of_interest.json"}) {
            if (!Files.isRegularFile(root.resolve(file), LinkOption.NOFOLLOW_LINKS)) return false;
        }
        try {
            var manifest = new JsonReader().parse(Files.readString(root.resolve("campaign.json")));
            boolean explicit = manifest.isObject() && manifest.getInt("schemaVersion", 0) == 1
                    && !manifest.getBoolean("inheritCommonWorld", true)
                    && !manifest.getBoolean("inheritCommonQuests", true);
            if (!explicit) return false;
            campaign.validateResources();
            return true;
        } catch (Exception error) { return false; }
    }

    private static void row(JPanel panel, int row, String name, Component component) {
        GridBagConstraints label = new GridBagConstraints();
        label.gridx = 0; label.gridy = row; label.anchor = GridBagConstraints.NORTHWEST; label.insets = new Insets(5, 0, 5, 12);
        panel.add(new JLabel(name), label);
        GridBagConstraints field = new GridBagConstraints();
        field.gridx = 1; field.gridy = row; field.weightx = 1; field.fill = GridBagConstraints.HORIZONTAL; field.insets = new Insets(5, 0, 5, 0);
        panel.add(component, field);
    }

    private void updateDestination() {
        String folder = title.getText().trim().replaceAll("[^A-Za-z0-9_-]+", "-").replaceAll("^-+|-+$", "");
        if (folder.isEmpty()) folder = "Campaign";
        if (folder.matches("(?i)CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]")) folder = "Campaign-" + folder;
        folder = folder.substring(0, Math.min(folder.length(), 48));
        Path target = projectsRoot.resolve(folder);
        destination.setText(target + (Files.exists(target) ? "\nThis folder exists; a unique suffix will be added." : "\nA unique suffix is added if the folder becomes occupied."));
        create.setEnabled(!busy && !title.getText().isBlank() && template.getSelectedItem() != null);
    }

    private void startCreation() {
        CampaignDefinition chosen = (CampaignDefinition) template.getSelectedItem();
        if (busy || chosen == null || title.getText().isBlank()) return;
        String campaignTitle = title.getText(), campaignDescription = description.getText();
        setBusy(true);
        new SwingWorker<CampaignDefinition, Void>() {
            @Override protected CampaignDefinition doInBackground() throws Exception {
                var project = CampaignProjectCreator.create(chosen.getRoot(), commonRoot, projectsRoot, campaignTitle, campaignDescription);
                return CampaignRegistry.readProjectMetadata(project.root(), commonRoot);
            }
            @Override protected void done() {
                try { created = get(); dispose(); }
                catch (Exception error) {
                    Throwable cause = error.getCause() == null ? error : error.getCause();
                    JOptionPane.showMessageDialog(NewCampaignWizard.this, "Could not create campaign: " + cause.getMessage(), "New Campaign", JOptionPane.ERROR_MESSAGE);
                } finally { setBusy(false); }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        title.setEnabled(!busy); description.setEnabled(!busy); template.setEnabled(!busy); cancel.setEnabled(!busy);
        status.setText(busy ? "Creating private campaign files… Please wait." : " ");
        updateDestination();
    }
}
