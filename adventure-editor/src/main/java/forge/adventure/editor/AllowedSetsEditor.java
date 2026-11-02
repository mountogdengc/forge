package forge.adventure.editor;

import forge.adventure.data.RewardData;
import forge.adventure.util.Config;
import forge.card.CardEdition;
import forge.localinstance.properties.ForgeConstants;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Campaign set choices use edition metadata, without initializing the card rule database. */
public final class AllowedSetsEditor extends JPanel {
    private final Config config;
    private final DefaultListModel<AllowedSetsSelection.Choice> model = new DefaultListModel<>();
    private final JList<AllowedSetsSelection.Choice> list = new JList<>(model);
    private final AllowedSetsSelection selection;
    private final JRadioButton allSets = new JRadioButton("All sets (subject to campaign restrictions)");
    private final JRadioButton curated = new JRadioButton("Only checked sets");
    private final JTextField search = new JTextField();
    private final JLabel restrictions = new JLabel(" ");
    private final JLabel status = new JLabel(" ");
    private boolean loaded;

    public AllowedSetsEditor(Config config) {
        super(new BorderLayout());
        this.config = config;
        selection = new AllowedSetsSelection(loadEditions());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer((items, choice, index, selected, focus) -> {
            JCheckBox checkbox = new JCheckBox(choice.toString(), selection.isChecked(choice.code));
            checkbox.setEnabled(!selection.isAllSets() && loaded);
            checkbox.setBackground(selected ? items.getSelectionBackground() : items.getBackground());
            checkbox.setForeground(selected ? items.getSelectionForeground() : items.getForeground());
            return checkbox;
        });
        list.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) {
                int index = list.locationToIndex(event.getPoint());
                if (index >= 0 && list.getCellBounds(index, index).contains(event.getPoint())) toggle(index);
            }
        });
        list.getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "toggle-set");
        list.getActionMap().put("toggle-set", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { toggle(list.getSelectedIndex()); }
        });
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        heading.add(new JLabel("Campaign: " + Path.of(config.getPrefix()).toAbsolutePath().normalize()));
        ButtonGroup policy = new ButtonGroup();
        policy.add(allSets);
        policy.add(curated);
        heading.add(allSets);
        heading.add(curated);
        allSets.addActionListener(e -> changePolicy(true));
        curated.addActionListener(e -> changePolicy(false));
        heading.add(restrictions);
        heading.add(new JLabel("Search sets by name or code (checked sets stay checked when hidden):"));
        heading.add(search);
        heading.add(new JLabel("Set policy filters generated card pools; authored decks and named rewards keep their specified cards."));
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
        add(heading, BorderLayout.NORTH);
        add(new JScrollPane(list), BorderLayout.CENTER);
        JPanel actions = new JPanel();
        JButton reload = new JButton("Reload from disk");
        reload.addActionListener(e -> selectConfiguredSets());
        JButton save = new JButton("Save allowed sets");
        save.addActionListener(e -> save());
        actions.add(reload);
        actions.add(save);
        actions.add(status);
        add(actions, BorderLayout.SOUTH);
        selectConfiguredSets();
    }

    private Map<String, String> loadEditions() {
        Map<String, String> choices = new LinkedHashMap<>();
        readEditions(new File(ForgeConstants.EDITIONS_DIR), false, choices);
        readEditions(new File(ForgeConstants.USER_CUSTOM_EDITIONS_DIR), true, choices);
        return choices;
    }

    private static void readEditions(File directory, boolean custom, Map<String, String> choices) {
        if (!directory.isDirectory()) return;
        for (CardEdition edition : new CardEdition.Reader(directory, custom).readAll().values()) {
            choices.put(edition.getCode(), edition.getName());
        }
    }

    private void selectConfiguredSets() {
        try {
            selection.reload(EditorFiles.read(config, "config.json").readString("UTF-8"));
            loaded = true;
            allSets.setSelected(selection.isAllSets());
            curated.setSelected(!selection.isAllSets());
            restrictions.setText(selection.restrictionSummary());
            restrictions.setToolTipText(selection.restrictionSummary());
            filter();
            status.setText("Loaded from disk. " + selection.checkedCount() + " checked.");
        } catch (Exception e) {
            loaded = false;
            list.repaint();
            status.setText("Cannot reload: " + e.getMessage());
        }
    }

    private void filter() {
        model.clear();
        selection.visible(search.getText()).forEach(model::addElement);
    }

    private void changePolicy(boolean all) {
        selection.setAllSets(all);
        list.repaint();
        status.setText("Unsaved policy. " + selection.checkedCount() + " checked (including hidden sets).");
    }

    private void toggle(int index) {
        if (!loaded || selection.isAllSets() || index < 0) return;
        String code = model.get(index).code;
        selection.setChecked(code, !selection.isChecked(code));
        list.repaint();
        status.setText("Unsaved selection. " + selection.checkedCount() + " checked (including hidden sets).");
    }

    private void save() {
        try {
            if (!loaded) throw new IllegalStateException("Reload a valid config.json before saving set choices.");
            List<String> codes = selection.codesToSave();
            Path destination = EditorFiles.writeTarget(Path.of(config.getPrefix()), "config.json").file().toPath();
            if (!java.nio.file.Files.exists(destination))
                java.nio.file.Files.writeString(destination, EditorFiles.read(config, "config.json").readString("UTF-8"),
                        java.nio.charset.StandardCharsets.UTF_8);
            AllowedSetsStore.save(destination, codes);
            config.getConfigData().allowedEditions = codes.isEmpty() ? null : codes.toArray(new String[0]);
            RewardData.invalidateCardPool();
            status.setText("Saved. " + (codes.isEmpty() ? "All sets mode." : codes.size() + " sets checked.")
                    + " Campaign restrictions still apply.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Cannot save allowed sets", JOptionPane.ERROR_MESSAGE);
        }
    }

}
