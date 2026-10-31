package forge.adventure.campaign;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Immutable identity and startup contract for one installed campaign. */
public final class CampaignDefinition {
    public static final int SCHEMA_VERSION = 1;

    private final String id;
    private final String version;
    private final String title;
    private final String description;
    private final String plane;
    private final Path root;
    private final Path commonRoot;
    private final String[] startingQuestIds;
    private final String startingLocation;
    private final boolean newGamePlusAllowed;
    private final boolean inheritCommonWorld;
    private final boolean inheritCommonQuests;
    private final boolean legacy;
    private final boolean development;

    CampaignDefinition(String id, String version, String title, String description, String plane,
                       Path root, Path commonRoot, String[] startingQuestIds, String startingLocation,
                       boolean newGamePlusAllowed, boolean inheritCommonWorld,
                       boolean inheritCommonQuests, boolean legacy, boolean development) {
        this.id = requireToken(id, "campaign ID");
        this.version = requireToken(version, "campaign version");
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("A campaign needs a title.");
        }
        this.title = title.trim();
        this.description = description == null ? "" : description.trim();
        this.plane = plane;
        this.root = root.toAbsolutePath().normalize();
        this.commonRoot = commonRoot.toAbsolutePath().normalize();
        this.startingQuestIds = startingQuestIds.clone();
        this.startingLocation = startingLocation == null || startingLocation.trim().isEmpty()
                ? null : startingLocation.trim();
        this.newGamePlusAllowed = newGamePlusAllowed;
        this.inheritCommonWorld = inheritCommonWorld;
        this.inheritCommonQuests = inheritCommonQuests;
        this.legacy = legacy;
        this.development = development;
    }

    private static String requireToken(String value, String label) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")) {
            throw new IllegalArgumentException("Invalid " + label + ": " + value);
        }
        return value;
    }

    public String getId() { return id; }
    public String getVersion() { return version; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getPlane() { return plane; }
    public Path getRoot() { return root; }
    public Path getCommonRoot() { return commonRoot; }
    public String[] getStartingQuestIds() { return startingQuestIds.clone(); }
    public String getStartingLocation() { return startingLocation; }
    public boolean isNewGamePlusAllowed() { return newGamePlusAllowed; }
    public boolean isLegacy() { return legacy; }
    public boolean isDevelopment() { return development; }

    /** Story and world inheritance must be declared; reusable assets keep the common fallback. */
    public boolean allowsCommonResource(String resource) {
        if ("world/world.json".equals(resource)) return inheritCommonWorld;
        if ("world/quests.json".equals(resource)) return inheritCommonQuests;
        return true;
    }

    public Path resolveResource(String resource) {
        Path local = root.resolve(resource).normalize();
        if (!local.startsWith(root)) {
            throw new IllegalArgumentException("Campaign resource escapes its directory: " + resource);
        }
        if (Files.isRegularFile(local)) return local;
        if (allowsCommonResource(resource)) {
            Path shared = commonRoot.resolve(resource).normalize();
            if (shared.startsWith(commonRoot) && Files.isRegularFile(shared)) return shared;
        }
        return null;
    }

    /** Runs without a game, graphics context, or changing the active campaign. */
    public void validateResources() {
        Path world = requireResource("world/world.json");
        Path quests = requireResource("world/quests.json");
        try {
            JsonValue worldData = new JsonReader().parse(Files.readString(world, StandardCharsets.UTF_8));
            if (!worldData.isObject() || worldData.getInt("width", 0) <= 0
                    || worldData.getInt("height", 0) <= 0 || worldData.getInt("tileSize", 0) <= 0) {
                throw new IllegalArgumentException("Campaign " + title + " has invalid world dimensions.");
            }
            Set<String> ids = readQuestIds(quests);
            Set<String> requested = new HashSet<>();
            for (String questId : startingQuestIds) {
                try {
                    if (Integer.parseInt(questId) <= 0) throw new NumberFormatException("Quest IDs must be positive");
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Campaign " + title + " has an invalid starting quest ID: " + questId + ".", ex);
                }
                if (questId == null || !ids.contains(questId)) {
                    throw new IllegalArgumentException("Campaign " + title + " is missing starting quest " + questId + ".");
                }
                if (!requested.add(questId)) {
                    throw new IllegalArgumentException("Campaign " + title + " repeats starting quest " + questId + ".");
                }
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Cannot read campaign resources for " + title + ".", ex);
        }
    }

    private Path requireResource(String path) {
        Path file = resolveResource(path);
        if (file == null) {
            throw new IllegalArgumentException("Campaign " + title + " is missing required resource " + path + ".");
        }
        return file;
    }

    static Set<String> readQuestIds(Path file) throws IOException {
        JsonValue values = new JsonReader().parse(Files.readString(file, StandardCharsets.UTF_8));
        if (!values.isArray()) throw new IllegalArgumentException("Campaign quests must be an array: " + file);
        Set<String> ids = new HashSet<>();
        for (JsonValue value = values.child; value != null; value = value.next) {
            JsonValue id = value.get("id");
            if (id != null && value.getBoolean("isTemplate", false) && !ids.add(id.asString())) {
                throw new IllegalArgumentException("Campaign has duplicate quest template ID " + id.asString() + ".");
            }
        }
        return ids;
    }

    @Override
    public String toString() { return title; }
}
