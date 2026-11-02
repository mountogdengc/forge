package forge.adventure.editor;

import forge.adventure.campaign.CampaignDefinition;
import forge.adventure.campaign.CampaignRegistry;
import forge.adventure.util.Config;
import forge.localinstance.properties.ForgeConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Campaign authoring entry point; reading a choice never activates a game. */
public final class CampaignChooserDialog extends JDialog {
    private final Path installedRoot, commonRoot, projectsRoot;
    private final DefaultListModel<CampaignDefinition> model = new DefaultListModel<>();
    private final JList<CampaignDefinition> campaigns = new JList<>(model);
    private final JTextArea details = new JTextArea(4, 50);
    private final JTextArea problems = new JTextArea(3, 50);
    private final JButton open = new JButton("Open Campaign");
    private CampaignDefinition selected;

    public static CampaignDefinition choose(String requestedId) {
        CampaignChooserDialog chooser = new CampaignChooserDialog(
                Path.of(Config.instance().getAdventureRoot()),
                Path.of(ForgeConstants.USER_ADVENTURE_DIR, "userplanes"), requestedId);
        chooser.setVisible(true);
        return chooser.selected;
    }

    CampaignChooserDialog(Path installedRoot, Path projectsRoot, String requestedId) {
        super((Frame) null, "Campaign Editor", true);
        this.installedRoot = installedRoot.toAbsolutePath().normalize();
        this.commonRoot = this.installedRoot.resolve("common");
        this.projectsRoot = projectsRoot.toAbsolutePath().normalize();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 4, 14));
        content.add(new JLabel("Choose a campaign to edit, or create an independent copy."), BorderLayout.NORTH);
        campaigns.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        campaigns.setVisibleRowCount(10);
        campaigns.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                      boolean chosen, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, chosen, focus);
                CampaignDefinition campaign = (CampaignDefinition) value;
                label.setText(campaign.getTitle() + "  |  " + campaign.getVersion() + "  |  " + campaign.getRoot().getFileName());
                label.setToolTipText(campaign.getRoot().toString());
                label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                return label;
            }
        });
        campaigns.addListSelectionListener(event -> selectionChanged());
        campaigns.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) { if (event.getClickCount() == 2 && open.isEnabled()) openSelected(); }
        });
        content.add(new JScrollPane(campaigns), BorderLayout.CENTER);
        details.setEditable(false); details.setLineWrap(true); details.setWrapStyleWord(true);
        content.add(new JScrollPane(details), BorderLayout.SOUTH);
        add(content, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout(6, 6));
        footer.setBorder(BorderFactory.createEmptyBorder(0, 14, 14, 14));
        problems.setEditable(false); problems.setLineWrap(true); problems.setWrapStyleWord(true);
        JScrollPane problemPane = new JScrollPane(problems);
        problemPane.setBorder(BorderFactory.createTitledBorder("Discovery notes"));
        footer.add(problemPane, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton create = new JButton("New Campaign"), refresh = new JButton("Refresh"), cancel = new JButton("Cancel");
        create.addActionListener(event -> newCampaign());
        refresh.addActionListener(event -> refresh(campaigns.getSelectedValue() == null ? requestedId : campaigns.getSelectedValue().getId()));
        open.addActionListener(event -> openSelected());
        cancel.addActionListener(event -> dispose());
        actions.add(create); actions.add(refresh); actions.add(open); actions.add(cancel);
        footer.add(actions, BorderLayout.SOUTH); add(footer, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(open);
        refresh(requestedId);
        pack(); setMinimumSize(new Dimension(650, 480)); setSize(Math.max(760, getWidth()), Math.max(560, getHeight()));
        setLocationRelativeTo(null);
    }

    private void refresh(String requestedId) {
        List<String> errors = new ArrayList<>();
        List<CampaignDefinition> discovered = discover(installedRoot, projectsRoot, errors);
        model.clear();
        for (CampaignDefinition campaign : discovered) model.addElement(campaign);
        int index = 0;
        for (int i = 0; i < model.size(); i++) if (model.get(i).getId().equals(requestedId)) index = i;
        if (!model.isEmpty()) campaigns.setSelectedIndex(index);
        problems.setText(errors.isEmpty() ? "Drafts can be opened even when content needs repair. Use Validate Campaign after opening."
                : String.join("\n", errors));
        problems.setCaretPosition(0);
        selectionChanged();
    }

    static List<CampaignDefinition> discover(Path installedRoot, Path projectsRoot, List<String> errors) {
        List<CampaignDefinition> found = new ArrayList<>();
        Path common = installedRoot.resolve("common");
        for (Path directory : new Path[]{installedRoot, projectsRoot}) {
            if (!Files.isDirectory(directory)) continue;
            boolean user = directory.equals(projectsRoot);
            try (var children = Files.list(directory)) {
                for (Path root : children.filter(Files::isDirectory).sorted().toList()) {
                    String folder = root.getFileName().toString();
                    if (folder.equals("common") || folder.startsWith(".")) continue;
                    try { found.add(CampaignRegistry.readProjectMetadata(root, common, user)); }
                    catch (Exception error) { errors.add(folder + ": " + error.getMessage()); }
                }
            } catch (Exception error) { errors.add(directory + ": " + error.getMessage()); }
        }
        found.sort(Comparator.comparing(CampaignDefinition::getTitle, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(c -> c.getRoot().toString()));
        return List.copyOf(found);
    }

    private void selectionChanged() {
        CampaignDefinition campaign = campaigns.getSelectedValue();
        open.setEnabled(campaign != null);
        details.setText(campaign == null ? "No campaigns found. Create one from an installed template."
                : campaign.getDescription() + "\n\nProject: " + campaign.getRoot());
        details.setCaretPosition(0);
    }

    private void openSelected() {
        selected = campaigns.getSelectedValue();
        if (selected != null) dispose();
    }

    private void newCampaign() {
        List<CampaignDefinition> templates = new ArrayList<>();
        for (int i = 0; i < model.size(); i++) if (NewCampaignWizard.isTemplate(model.get(i))) templates.add(model.get(i));
        templates.sort(Comparator.comparingInt((CampaignDefinition c) -> c.getId().equals("fork.bellwake") ? 0 : 1)
                .thenComparing(CampaignDefinition::getTitle, String.CASE_INSENSITIVE_ORDER));
        if (templates.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No complete local templates were found. Bellwake and Curse of Strahd need their own campaign, config, world, quest, enemy and location files.", "New Campaign", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        CampaignDefinition created = NewCampaignWizard.create(this, commonRoot, projectsRoot, templates);
        if (created != null) { selected = created; dispose(); }
    }
}
