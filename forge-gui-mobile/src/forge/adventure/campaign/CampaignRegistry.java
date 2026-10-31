package forge.adventure.campaign;

import com.badlogic.gdx.utils.Json;
import forge.adventure.util.Config;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/** Discovers installed definitions without activating their configuration or resources. */
public final class CampaignRegistry {
    private static final Set<String> DEVELOPMENT_PLANES = Set.of("Amonkhet", "Innistrad", "Crystal_Kingdoms");
    private final List<CampaignDefinition> campaigns;
    private final List<String> errors;

    private CampaignRegistry(List<CampaignDefinition> campaigns, List<String> errors) {
        campaigns.sort(Comparator.comparing(CampaignDefinition::getTitle, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(CampaignDefinition::getId));
        this.campaigns = Collections.unmodifiableList(campaigns);
        this.errors = Collections.unmodifiableList(errors);
    }

    public static CampaignRegistry discover() {
        boolean development = FModel.getPreferences() != null
                && FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.DEV_MODE_ENABLED);
        return discover(Path.of(Config.instance().getAdventureRoot()),
                Path.of(ForgeConstants.USER_ADVENTURE_DIR, "userplanes"), development);
    }

    public static CampaignRegistry discover(Path installedRoot, Path userRoot, boolean includeDevelopment) {
        Map<String, CampaignDefinition> found = new LinkedHashMap<>();
        Set<String> ambiguous = new HashSet<>();
        List<String> errors = new ArrayList<>();
        scan(installedRoot, installedRoot.resolve("common"), false, includeDevelopment, found, ambiguous, errors);
        scan(userRoot, installedRoot.resolve("common"), true, includeDevelopment, found, ambiguous, errors);
        return new CampaignRegistry(new ArrayList<>(found.values()), errors);
    }

    private static void scan(Path directory, Path commonRoot, boolean user, boolean includeDevelopment,
                             Map<String, CampaignDefinition> found, Set<String> ambiguous, List<String> errors) {
        if (!Files.isDirectory(directory)) return;
        try (Stream<Path> directories = Files.list(directory)) {
            List<Path> roots = directories.filter(Files::isDirectory).sorted().toList();
            for (Path root : roots) {
                String name = root.getFileName().toString();
                if (name.equals("common") || name.startsWith(".")) continue;
                String plane = (user ? "<user>" : "") + name;
                try {
                    CampaignDefinition definition = readDefinition(root, commonRoot, plane, user);
                    if (definition.isDevelopment() && !includeDevelopment) continue;
                    definition.validateResources();
                    if (ambiguous.contains(definition.getId()) || found.containsKey(definition.getId())) {
                        found.remove(definition.getId());
                        ambiguous.add(definition.getId());
                        errors.add("Duplicate campaign ID " + definition.getId() + ". Give each installed campaign a unique ID.");
                    } else {
                        found.put(definition.getId(), definition);
                    }
                } catch (RuntimeException | IOException ex) {
                    errors.add(name + ": " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
                }
            }
        } catch (IOException ex) {
            errors.add("Cannot list campaigns in " + directory + ": " + ex.getMessage());
        }
    }

    private static CampaignDefinition readDefinition(Path root, Path commonRoot, String plane, boolean user) throws IOException {
        Path file = root.resolve("campaign.json");
        if (!Files.isRegularFile(file)) return legacyDefinition(root, commonRoot, plane, user);
        Manifest manifest = new Json().fromJson(Manifest.class, Files.readString(file, StandardCharsets.UTF_8));
        if (manifest == null || manifest.schemaVersion != CampaignDefinition.SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported campaign descriptor schema; expected " + CampaignDefinition.SCHEMA_VERSION + ".");
        }
        if (manifest.startingQuestIds == null) {
            throw new IllegalArgumentException("Declare startingQuestIds, using [] for a campaign without an opening quest.");
        }
        return new CampaignDefinition(manifest.id, manifest.version, manifest.title, manifest.description,
                plane, root, commonRoot, manifest.startingQuestIds, manifest.startingLocation,
                manifest.allowNewGamePlus, manifest.inheritCommonWorld, manifest.inheritCommonQuests,
                false, manifest.development);
    }

    private static CampaignDefinition legacyDefinition(Path root, Path commonRoot, String plane, boolean user) throws IOException {
        Path quests = root.resolve("world/quests.json");
        if (!Files.isRegularFile(quests)) quests = commonRoot.resolve("world/quests.json");
        // Only the legacy adapter retains the historic Shandalar startup convention.
        String[] startingQuests = Files.isRegularFile(quests) && CampaignDefinition.readQuestIds(quests).contains("28")
                ? new String[]{"28"} : new String[0];
        String origin = (user ? "user:" : "builtin:") + root.getFileName();
        String id = "legacy." + UUID.nameUUIDFromBytes(origin.getBytes(StandardCharsets.UTF_8));
        String title = root.getFileName().toString().replace('_', ' ');
        return new CampaignDefinition(id, "legacy-1", title,
                "Explore the existing Forge Adventure. Each new game starts a separate character and save history.",
                plane, root, commonRoot, startingQuests, "Spawn", true, true, true, true,
                !user && DEVELOPMENT_PLANES.contains(root.getFileName().toString()));
    }

    public List<CampaignDefinition> getCampaigns() { return campaigns; }
    public List<String> getErrors() { return errors; }

    public CampaignDefinition find(String id, String version) {
        CampaignDefinition definition = findById(id);
        return definition != null && definition.getVersion().equals(version) ? definition : null;
    }

    public CampaignDefinition findById(String id) {
        return campaigns.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
    }

    public CampaignDefinition findByPlane(String plane) {
        return campaigns.stream().filter(c -> c.getPlane().equals(plane)
                || c.getPlane().replace("<user>", "user_").equals(plane)).findFirst().orElse(null);
    }

    /** JSON transport only. Runtime code uses the validated immutable definition. */
    public static final class Manifest {
        public int schemaVersion;
        public String id;
        public String version;
        public String title;
        public String description;
        public String[] startingQuestIds;
        public String startingLocation;
        public boolean allowNewGamePlus;
        public boolean inheritCommonWorld;
        public boolean inheritCommonQuests;
        public boolean development;
    }
}
