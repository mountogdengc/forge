package forge.adventure.editor;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import forge.adventure.campaign.CampaignDefinition;
import forge.adventure.campaign.CampaignMapResolver;
import forge.adventure.campaign.CampaignRegistry;
import forge.adventure.util.Config;
import forge.localinstance.properties.ForgeConstants;
import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/** The selected project's identity, connected tools and offline checks. */
final class CampaignOverviewPanel extends JPanel {
    private CampaignDefinition campaign;
    private final Config config;
    private final Consumer<String> navigate;
    private final Runnable titleChanged;
    private final JTextArea summary = new JTextArea();
    final JTextArea results = new JTextArea("Validate before playtesting. The first check builds a reusable card-name index.");
    final JButton validate = new JButton("Validate Campaign");
    final DefaultListModel<Location> locations = new DefaultListModel<>();
    final JList<Location> locationList = new JList<>(locations);
    private final CampaignValidationService validation;
    private boolean validationQueued;
    private Runnable beforePrivateMaps = () -> { }, afterPrivateMaps = () -> { };
    private Consumer<String> checkStatus = value -> { };

    CampaignOverviewPanel(Config config, Consumer<String> navigate, Runnable titleChanged) {
        super(new BorderLayout(8, 8));
        this.config = config;
        this.campaign = config.getCampaign();
        this.navigate = navigate;
        this.titleChanged = titleChanged;
        validation = new CampaignValidationService(Path.of(config.getAdventureRoot()).getParent(),
                Path.of(ForgeConstants.CACHE_DIR, "adventure", "editor-card-index"));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        summary.setEditable(false); summary.setLineWrap(true); summary.setWrapStyleWord(true);
        summary.setRows(8);
        add(new JScrollPane(summary), BorderLayout.NORTH);
        JPanel middle = new JPanel(new BorderLayout(6, 6));
        middle.add(new JLabel("Locations (maps in a new campaign copy belong to that project)"), BorderLayout.NORTH);
        middle.add(new JScrollPane(locationList), BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(0, 4, 4, 4));
        button(actions, "Edit Details", this::editDetails);
        button(actions, "Open Project Folder", () -> open(campaign.getRoot()));
        button(actions, "Open Selected Map", this::openMap);
        button(actions, "Create Editable Map Copies", this::makeMapsPrivate).setToolTipText(
                "<html>Copy this campaign's maps so you can edit them without affecting other campaigns.<br>"
                + "Quests, settings, and campaign identity stay the same.</html>");
        button(actions, "World", () -> navigate.accept("World"));
        button(actions, "Items", () -> navigate.accept("Items"));
        button(actions, "Enemies", () -> navigate.accept("Enemies"));
        button(actions, "Files / Repairs", () -> navigate.accept("Files"));
        button(actions, "Edit Locations", () -> navigate.accept("Locations"));
        button(actions, "Edit Quests", () -> navigate.accept("Quests"));
        button(actions, "Allowed Sets", () -> navigate.accept("Allowed sets"));
        middle.add(actions, BorderLayout.SOUTH); add(middle, BorderLayout.CENTER);
        JPanel checks = new JPanel(new BorderLayout(6, 6));
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT)); header.add(validate);
        header.add(new JLabel("Save each tool before validating. Restart the game to play a separate new run."));
        checks.add(header, BorderLayout.NORTH);
        results.setEditable(false); results.setLineWrap(true); results.setWrapStyleWord(true); results.setRows(8);
        checks.add(new JScrollPane(results), BorderLayout.CENTER); add(checks, BorderLayout.SOUTH);
        validate.addActionListener(e -> validateCampaign());
        refresh();
    }
    private static JButton button(JPanel panel, String title, Runnable action) {
        JButton button = new JButton(title); button.addActionListener(e -> action.run()); panel.add(button); return button;
    }
    void refresh() {
        try { campaign = CampaignRegistry.readProjectMetadata(campaign.getRoot(), campaign.getCommonRoot(), campaign.getPlane().startsWith("<user>")); }
        catch (Exception ignored) { /* Keep the last readable metadata while Files repairs the descriptor. */ }
        summary.setText(campaign.getTitle() + "\n" + campaign.getDescription() + "\n\nIdentity: " + campaign.getId()
                + "    Version: " + campaign.getVersion() + "\nProject: " + campaign.getRoot()
                + "\nStart: " + campaign.getStartingLocation() + "    Opening quests: "
                + String.join(", ", campaign.getStartingQuestIds())
                + "\nChanges affect this project's future runs. Keep the same identity for updates; use New Campaign for a separate adventure.");
        locations.clear();
        try {
            JsonValue points = new JsonReader().parse(EditorFiles.read(config, "world/points_of_interest.json"));
            for (JsonValue point = points.child; point != null; point = point.next) {
                String map = point.getString("map", "");
                locations.addElement(new Location(point.getString("name", "Unnamed location"), map));
            }
        } catch (Exception error) { results.setText("world/points_of_interest.json: " + error.getMessage()); }
        if (!locations.isEmpty()) locationList.setSelectedIndex(0);
    }
    void setMapLifecycle(Runnable before, Runnable after) { beforePrivateMaps = before; afterPrivateMaps = after; }
    void setCheckStatus(Consumer<String> status) { checkStatus = status; }
    void validateCampaign() {
        if (!validate.isEnabled()) { validationQueued = true; checkStatus.accept("Changes pending another campaign check"); return; }
        checkStatus.accept("Checking saved campaign files...");
        validate.setEnabled(false); results.setText("Checking project files, maps, decks and card references...");
        new SwingWorker<List<String>, Void>() {
            protected List<String> doInBackground() { return validation.validate(campaign.getRoot(), campaign.getCommonRoot()); }
            protected void done() {
                try {
                    List<String> errors = get();
                    if (validationQueued) return;
                    checkStatus.accept(errors.isEmpty() ? "Saved-file checks passed" : errors.size() + " campaign issue(s) - see Overview");
                    results.setText(errors.isEmpty() ? "Validation passed. Saved campaign files and referenced cards passed the available checks.\nNext: restart the game, select this campaign and start a new run for a manual playtest."
                            : errors.size() + " issue(s):\n\n" + String.join("\n\n", errors));
                    results.setCaretPosition(0);
                } catch (Exception error) { results.setText("Validation could not complete: " + error.getMessage()); checkStatus.accept("Campaign checks could not complete - see Overview"); }
                finally { validate.setEnabled(true); if (validationQueued) { validationQueued = false; validateCampaign(); } }
            }
        }.execute();
    }
    private void editDetails() {
        JTextField title = new JTextField(campaign.getTitle(), 36);
        JTextField version = new JTextField(campaign.getVersion());
        JTextArea description = new JTextArea(campaign.getDescription(), 4, 36); description.setLineWrap(true); description.setWrapStyleWord(true);
        JPanel fields = new JPanel(new GridLayout(0, 1, 4, 4));
        fields.add(new JLabel("Title")); fields.add(title); fields.add(new JLabel("Description")); fields.add(new JScrollPane(description));
        fields.add(new JLabel("Version (changing this can require a saved-run migration)")); fields.add(version);
        if (JOptionPane.showConfirmDialog(this, fields, "Campaign Details", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            CampaignMetadataStore.save(campaign, title.getText(), description.getText(), version.getText());
            campaign = CampaignRegistry.readProjectMetadata(campaign.getRoot(), campaign.getCommonRoot(), campaign.getPlane().startsWith("<user>"));
            refresh(); titleChanged.run();
            results.setText("Campaign details saved. Restart the game to see updated metadata.");
        } catch (Exception error) { message("Could not save details: " + error.getMessage()); }
    }
    private void validateQueuedCheck() { validate.setEnabled(true); validationQueued = false; validateCampaign(); }
    String campaignTitle() { return campaign.getTitle(); }
    private void makeMapsPrivate() {
        if (!validate.isEnabled()) { message("Wait for campaign checks to finish, then try again."); return; }
        try { beforePrivateMaps.run(); } catch (RuntimeException error) { message("Could not save Locations before copying maps: " + error.getMessage()); return; }
        validate.setEnabled(false);
        results.setText("Copying maps and dependencies into this campaign. Identity and version will stay the same...");
        new SwingWorker<Path, Void>() {
            protected Path doInBackground() throws Exception { return CampaignProjectCreator.makeMapsPrivate(campaign.getRoot(), campaign.getCommonRoot()); }
            protected void done() {
                Exception failure = null;
                try { get(); } catch (Exception error) { failure = error; }
                try { afterPrivateMaps.run(); } catch (RuntimeException error) { if (failure == null) failure = error; }
                if (failure == null) { refresh(); validateQueuedCheck(); }
                else { results.setText("Could not make maps private: " + failure.getMessage()); validate.setEnabled(true); }
            }
        }.execute();
    }
    private void openMap() {
        Location location = locationList.getSelectedValue();
        if (location == null) { message("Select a location first."); return; }
        if (!location.map.startsWith(CampaignMapResolver.PREFIX)) {
            message("This location uses a shared map. Click Create Editable Map Copies here, then open the map. A new campaign or wizard is not required."); return;
        }
        try { open(CampaignMapResolver.resolve(campaign.getRoot(), campaign.getCommonRoot(), location.map)); }
        catch (Exception error) { message(error.getMessage()); }
    }
    private void open(Path file) {
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
                throw new IllegalStateException("Opening files is unavailable on this desktop.");
            Desktop.getDesktop().open(file.toFile());
        } catch (Exception error) { message("Could not open " + file + ": " + error.getMessage() + "\nInstall Tiled and associate .tmx files to edit maps."); }
    }
    private void message(String message) { JOptionPane.showMessageDialog(this, message, "Campaign Editor", JOptionPane.INFORMATION_MESSAGE); }
    record Location(String name, String map) {
        @Override public String toString() { return name + " - " + (map.startsWith(CampaignMapResolver.PREFIX) ? map.substring(CampaignMapResolver.PREFIX.length()) : "shared: " + map); }
    }
}
