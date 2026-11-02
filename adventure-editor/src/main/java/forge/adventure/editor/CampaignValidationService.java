package forge.adventure.editor;

import forge.CardStorageReader;
import forge.adventure.campaign.CampaignContentValidator;
import forge.adventure.campaign.CampaignRegistry;
import forge.adventure.campaign.CampaignDefinition;
import forge.adventure.data.SettingData;
import forge.adventure.util.Config;
import forge.card.CardType;
import forge.card.CardRules;
import forge.util.FileSection;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Offline campaign checks, with a lazy card-name index instead of a game database. */
public final class CampaignValidationService {
    private static final int MAX_CACHED_NAMES = 512;
    private final Path resourcesRoot;
    private final Path cacheDirectory;
    private CardStorageReader cardReader;
    // Keep only bounded named lookup results, never CardRules or a full card catalog.
    private final Map<String, Boolean> cardNames = new LinkedHashMap<>(32, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > MAX_CACHED_NAMES;
        }
    };

    public CampaignValidationService(Path resourcesRoot, Path cacheDirectory) {
        this.resourcesRoot = Objects.requireNonNull(resourcesRoot).toAbsolutePath().normalize();
        this.cacheDirectory = Objects.requireNonNull(cacheDirectory).toAbsolutePath().normalize();
    }

    /** Called by the editor's background worker; never activates or writes a campaign. */
    public synchronized List<String> validate(Path projectRoot, Path commonRoot) {
        Path project = projectRoot.toAbsolutePath().normalize();
        Path common = commonRoot.toAbsolutePath().normalize();
        List<String> errors = new ArrayList<>();
        CampaignDefinition definition = null;
        try {
            definition = CampaignRegistry.readProject(project, common);
        } catch (IOException | RuntimeException error) {
            errors.add("campaign.json: " + message(error));
        }
        if (definition != null) {
            try {
                // Validate the same creation settings used by a new game, without
                // installing this preview or consulting the active user's settings.
                Config.forCampaign(definition, new SettingData());
            } catch (RuntimeException error) {
                errors.add("config.json: " + message(error));
            }
        }
        try {
            errors.addAll(CampaignContentValidator.validate(project, common, this::cardExists));
        } catch (RuntimeException error) {
            errors.add("Campaign validation could not complete: " + message(error));
        }
        return errors.stream().map(error -> relative(error, project, common)).distinct().toList();
    }

    private boolean cardExists(String name) {
        Boolean cached = cardNames.get(name);
        if (cached != null) return cached;
        if (cardReader == null) {
            initializeTypes();
            cardReader = new CardStorageReader(resourcesRoot.resolve("cardsfolder").toString(), null, true, cacheDirectory);
        }
        CardRules rules = cardReader.attemptToLoadCard(name);
        boolean exists = rules != null && matchesActualName(rules, name);
        cardNames.put(name, exists);
        return exists;
    }

    private static boolean matchesActualName(CardRules rules, String requested) {
        // The storage index normalizes filenames aggressively. A successful script
        // lookup alone does not mean that the requested spelling is a card name.
        if (sameName(requested, rules.getPreInitName())) return true;
        for (var face : rules.getAllFaces()) {
            if (face != null && (sameName(requested, face.getName()) || sameName(requested, face.getFlavorName()))) return true;
        }
        for (String placeholder : rules.getPlaceholderFaceNames()) if (sameName(requested, placeholder)) return true;
        if (rules.getPlaceholderFaceNames().isEmpty() && rules.getSupportedFunctionalVariants() != null) {
            for (String variant : rules.getSupportedFunctionalVariants()) {
                if (sameName(requested, rules.getDisplayNameForVariant(variant))) return true;
            }
        }
        return false;
    }

    private static boolean sameName(String requested, String actual) {
        // CardDb is case insensitive and accepts accent-free aliases of actual names.
        return actual != null && (actual.equalsIgnoreCase(requested)
                || StringUtils.stripAccents(actual).equalsIgnoreCase(requested));
    }

    private void initializeTypes() {
        synchronized (CardType.Constant.LOADED) {
            if (CardType.Constant.LOADED.isSet()) return;
            Path types = resourcesRoot.resolve("lists/TypeLists.txt");
            try {
                var sections = FileSection.parseSections(Files.readAllLines(types));
                if (sections.isEmpty()) throw new IOException("No card type definitions found.");
                for (var section : sections.entrySet()) CardType.Helper.parseTypes(section.getKey(), section.getValue());
                CardType.Constant.LOADED.set();
            } catch (IOException error) {
                throw new IllegalStateException("Cannot initialize card types from " + types, error);
            }
        }
    }

    private static String message(Exception error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private static String relative(String error, Path project, Path common) {
        return error.replace(project + java.io.File.separator, "")
                .replace(common + java.io.File.separator, "common/");
    }
}
