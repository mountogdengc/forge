package forge.adventure.editor;

import forge.adventure.data.RewardData;
import forge.adventure.util.Config;
import forge.card.CardEdition;
import forge.localinstance.properties.ForgeConstants;

import javax.swing.*;
import java.awt.BorderLayout;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Campaign set choices use edition metadata, without initializing the card rule database. */
public final class AllowedSetsEditor extends JPanel {
    private final Config config;
    private final DefaultListModel<EditionChoice> model = new DefaultListModel<>();
    private final JList<EditionChoice> list = new JList<>(model);
    private final JLabel status = new JLabel(" ");

    public AllowedSetsEditor(Config config) {
        super(new BorderLayout());
        this.config = config;
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JPanel heading = new JPanel(new BorderLayout());
        heading.add(new JLabel("Allowed sets — no selection uses all sets with the campaign's existing restrictions."), BorderLayout.NORTH);
        heading.add(new JLabel("Campaign: " + Path.of(config.getPrefix()).toAbsolutePath().normalize()), BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH);
        add(new JScrollPane(list), BorderLayout.CENTER);
        JPanel actions = new JPanel();
        JButton all = new JButton("All sets (clear selection)");
        all.addActionListener(e -> list.clearSelection());
        JButton reload = new JButton("Reload selection");
        reload.addActionListener(e -> selectConfiguredSets());
        JButton save = new JButton("Save allowed sets");
        save.addActionListener(e -> save());
        actions.add(all);
        actions.add(reload);
        actions.add(save);
        actions.add(status);
        add(actions, BorderLayout.SOUTH);
        loadEditions();
        selectConfiguredSets();
    }

    private void loadEditions() {
        Map<String, EditionChoice> choices = new LinkedHashMap<>();
        readEditions(new File(ForgeConstants.EDITIONS_DIR), false, choices);
        readEditions(new File(ForgeConstants.USER_CUSTOM_EDITIONS_DIR), true, choices);
        // Keep unknown/custom authored codes selectable rather than silently dropping them on save.
        String[] allowed = config.getConfigData().allowedEditions;
        if (allowed != null) for (String code : allowed) choices.putIfAbsent(code, new EditionChoice(code, "Unavailable edition"));
        List<EditionChoice> sorted = new ArrayList<>(choices.values());
        sorted.sort(Comparator.comparing(choice -> choice.name, String.CASE_INSENSITIVE_ORDER));
        sorted.forEach(model::addElement);
    }

    private static void readEditions(File directory, boolean custom, Map<String, EditionChoice> choices) {
        if (!directory.isDirectory()) return;
        for (CardEdition edition : new CardEdition.Reader(directory, custom).readAll().values()) {
            choices.put(edition.getCode(), new EditionChoice(edition.getCode(), edition.getName()));
        }
    }

    private void selectConfiguredSets() {
        list.clearSelection();
        String[] allowed = config.getConfigData().allowedEditions;
        if (allowed == null) return;
        List<String> selected = Arrays.asList(allowed);
        for (int i = 0; i < model.size(); i++) {
            if (selected.contains(model.get(i).code)) list.addSelectionInterval(i, i);
        }
    }

    private void save() {
        List<String> codes = new ArrayList<>();
        for (EditionChoice choice : list.getSelectedValuesList()) codes.add(choice.code);
        try {
            AllowedSetsStore.save(Path.of(config.getPrefix(), "config.json"), codes);
            config.getConfigData().allowedEditions = codes.isEmpty() ? null : codes.toArray(new String[0]);
            RewardData.invalidateCardPool();
            status.setText("Saved. " + (codes.isEmpty() ? "All sets allowed." : codes.size() + " sets selected."));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Cannot save allowed sets", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static final class EditionChoice {
        final String code;
        final String name;
        EditionChoice(String code, String name) { this.code = code; this.name = name; }
        @Override public String toString() { return name + " (" + code + ")"; }
    }
}
