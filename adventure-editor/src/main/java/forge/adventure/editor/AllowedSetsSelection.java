package forge.adventure.editor;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.*;

/** Selection state is independent of the currently visible search results. */
final class AllowedSetsSelection {
    static final class Choice {
        final String code;
        final String name;
        Choice(String code, String name) { this.code = code; this.name = name; }
        @Override public String toString() { return name + " (" + code + ")"; }
    }
    private final Map<String, String> editions;
    private final Set<String> checked = new LinkedHashSet<>();
    private List<Choice> choices = List.of();
    private boolean allSets = true;
    private String restrictions = "";

    AllowedSetsSelection(Map<String, String> editions) { this.editions = new LinkedHashMap<>(editions); }

    void reload(String json) {
        JsonValue document = new JsonReader().parse(json);
        if (!document.isObject()) throw new IllegalArgumentException("Campaign configuration must be an object.");
        String[] allowed = codes(document, "allowedEditions");
        String[] restricted = codes(document, "restrictedEditions");
        Map<String, String> available = new LinkedHashMap<>(editions);
        for (String code : allowed) available.putIfAbsent(code, "Unavailable edition — preserved from config");
        List<Choice> refreshed = new ArrayList<>();
        available.forEach((code, name) -> refreshed.add(new Choice(code, name)));
        refreshed.sort(Comparator.comparing((Choice choice) -> choice.name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(choice -> choice.code));
        checked.clear();
        Collections.addAll(checked, allowed);
        choices = refreshed;
        allSets = checked.isEmpty();
        restrictions = restricted.length == 0 ? "No restrictedEditions configured."
                : "restrictedEditions still exclude: " + String.join(", ", restricted);
    }

    private static String[] codes(JsonValue document, String field) {
        JsonValue value = document.get(field);
        if (value == null || value.isNull()) return new String[0];
        if (!value.isArray()) throw new IllegalArgumentException(field + " must be an array of edition codes.");
        List<String> result = new ArrayList<>();
        for (JsonValue code = value.child; code != null; code = code.next) {
            if (!code.isString()) throw new IllegalArgumentException(field + " must contain edition codes as strings.");
            result.add(code.asString());
        }
        return result.toArray(new String[0]);
    }

    List<Choice> visible(String query) {
        String search = query.trim().toLowerCase(Locale.ROOT);
        List<Choice> visible = new ArrayList<>();
        for (Choice choice : choices) {
            if (choice.toString().toLowerCase(Locale.ROOT).contains(search)) visible.add(choice);
        }
        return visible;
    }
    void setChecked(String code, boolean selected) {
        if (selected) checked.add(code); else checked.remove(code);
    }
    boolean isChecked(String code) { return checked.contains(code); }
    void setAllSets(boolean all) { allSets = all; }
    boolean isAllSets() { return allSets; }
    int checkedCount() { return checked.size(); }
    List<String> codesToSave() {
        if (allSets) return List.of();
        if (checked.isEmpty()) throw new IllegalArgumentException("Check at least one set, or choose All sets.");
        return new ArrayList<>(checked);
    }
    String restrictionSummary() { return restrictions; }
}
