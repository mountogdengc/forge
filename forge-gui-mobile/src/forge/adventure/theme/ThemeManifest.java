package forge.adventure.theme;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.*;

/** Versioned, strict JSON. This class has no game or graphics dependencies. */
public final class ThemeManifest {
    public static final Set<String> COLOR_ROLES = Set.of(
            "surface.canvas", "surface.panel", "surface.paper", "text.primary", "text.secondary", "text.onPaper",
            "border.frame", "border.frameText", "border.highlight", "action.primary", "action.onPrimary",
            "action.secondary", "action.onSecondary", "state.hover", "state.selected", "state.onSelected",
            "state.disabled", "state.onDisabled", "state.focus", "phase.activeEnabled", "phase.activeDisabled",
            "phase.inactiveEnabled", "phase.inactiveDisabled", "target.normal", "target.combat", "target.planeswalker");
    public static final Set<String> ASSET_SLOTS = Set.of("panel", "paper", "button", "buttonPressed", "buttonHover",
            "buttonDisabled", "buttonPrimary", "tab", "selection", "dialog", "medallion", "minimap", "hud",
            "resource", "glass");
    private static final Set<String> SECTIONS = Set.of("id", "name", "schemaVersion", "extends", "colors", "fonts",
            "materials", "components", "presentation");
    private final JsonObject document;
    private ThemeManifest(JsonObject document) { this.document = document.deepCopy(); }

    public static ThemeManifest parse(Path file, String json) {
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setStrictness(Strictness.STRICT);
            JsonElement element = JsonParser.parseReader(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT || !element.isJsonObject()) throw new IllegalArgumentException("Expected one JSON object");
            JsonObject doc = element.getAsJsonObject();
            for (String key : doc.keySet()) if (!SECTIONS.contains(key)) throw new IllegalArgumentException("Unsupported field: " + key);
            if (!doc.has("schemaVersion") || !doc.get("schemaVersion").toString().equals("1")) throw new IllegalArgumentException("schemaVersion must be 1");
            String id = requiredString(doc, "id");
            if (!id.matches("[a-z][a-z0-9._-]{0,79}") || id.equals("campaign")) throw new IllegalArgumentException("Invalid or reserved id: " + id);
            if (requiredString(doc, "name").length() > 120) throw new IllegalArgumentException("name is too long");
            if (doc.has("extends") && !requiredString(doc, "extends").matches("[a-z][a-z0-9._-]{0,79}")) throw new IllegalArgumentException("Invalid parent id");
            for (String section : List.of("colors", "fonts", "materials", "components", "presentation")) {
                if (!doc.has(section)) continue;
                if (!doc.get(section).isJsonObject()) throw new IllegalArgumentException(section + " must be an object");
                for (var entry : doc.getAsJsonObject(section).entrySet()) {
                    if (!entry.getValue().isJsonPrimitive()) throw new IllegalArgumentException(section + "." + entry.getKey() + " must be a value");
                    if (!(section.equals("presentation") && entry.getKey().equals("paperTexture")) &&
                            !entry.getValue().getAsJsonPrimitive().isString()) throw new IllegalArgumentException(section + "." + entry.getKey() + " must be a string");
                }
            }
            return new ThemeManifest(doc);
        } catch (Exception e) { throw new IllegalArgumentException(file + " [manifest]: " + e.getMessage(), e); }
    }
    private static String requiredString(JsonObject doc, String key) {
        if (!doc.has(key) || !doc.get(key).isJsonPrimitive() || !doc.getAsJsonPrimitive(key).isString() || doc.get(key).getAsString().isBlank())
            throw new IllegalArgumentException(key + " must be a nonempty string");
        return doc.get(key).getAsString();
    }
    public String id() { return document.get("id").getAsString(); }
    public String name() { return document.get("name").getAsString(); }
    public String parent() { return document.has("extends") ? document.get("extends").getAsString() : (id().equals("folio") ? null : "folio"); }
    public Map<String,String> section(String name) {
        Map<String,String> result = new LinkedHashMap<>();
        if (document.has(name)) for (var e : document.getAsJsonObject(name).entrySet()) result.put(e.getKey(), e.getValue().getAsString());
        return Collections.unmodifiableMap(result);
    }
    public JsonObject json() { return document.deepCopy(); }
    public String toJson() { return new GsonBuilder().setPrettyPrinting().create().toJson(document); }
}
