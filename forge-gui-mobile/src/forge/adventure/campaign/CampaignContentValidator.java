package forge.adventure.campaign;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Offline reference checks using Forge's permissive JSON parser; no world or graphics allocation. */
public final class CampaignContentValidator {
    private final Path root;
    private final Path common;
    private final Predicate<String> cardExists;
    private final List<String> errors = new ArrayList<>();
    private final Set<Path> checked = new HashSet<>();
    private final Set<String> enemies = new HashSet<>();

    private CampaignContentValidator(Path root, Path common, Predicate<String> cardExists) {
        this.root = root.toAbsolutePath().normalize();
        this.common = common.toAbsolutePath().normalize();
        this.cardExists = java.util.Objects.requireNonNull(cardExists);
    }

    public static List<String> validate(Path root, Path common, Predicate<String> cardExists) {
        CampaignContentValidator validator = new CampaignContentValidator(root, common, cardExists);
        validator.validate();
        return List.copyOf(validator.errors);
    }

    private JsonValue json(Path file) {
        try { return new JsonReader().parse(Files.readString(file)); }
        catch (Exception e) { errors.add(file + ": cannot parse resource: " + e.getMessage()); return new JsonValue(JsonValue.ValueType.array); }
    }

    private Path resource(String name, String source) {
        if (name == null || name.isBlank()) { errors.add(source + ": missing resource name"); return null; }
        for (Path base : new Path[]{root, common}) {
            Path file = base.resolve(name).normalize();
            if (file.startsWith(base) && Files.isRegularFile(file)) return file;
        }
        errors.add(source + ": missing resource " + name);
        return null;
    }

    private void validate() {
        Path manifestFile = resource("campaign.json", "campaign");
        JsonValue manifest = manifestFile == null ? new JsonValue(JsonValue.ValueType.object) : json(manifestFile);
        Path questFile = root.resolve("world/quests.json");
        if (!Files.isRegularFile(questFile) && manifest.getBoolean("inheritCommonQuests", false)) questFile = common.resolve("world/quests.json");
        Set<Integer> questIds = new HashSet<>();
        if (!Files.isRegularFile(questFile)) errors.add(questFile + ": missing quest catalog");
        else for (JsonValue quest : json(questFile)) {
            int id = quest.getInt("id", -1);
            if (!questIds.add(id)) errors.add(questFile + ": duplicate quest " + id);
            validateStages(quest, questFile + " quest " + id);
            for (String key : new String[]{"prologue", "epilogue", "failureDialog", "offerDialog"}) {
                if (quest.has(key)) dialog(quest.get(key), questFile + " quest " + id, null, 0);
            }
        }
        JsonValue openings = manifest.get("startingQuestIds");
        if (openings != null) for (JsonValue opening : openings) {
            try { if (!questIds.contains(Integer.parseInt(opening.asString()))) errors.add(manifestFile + ": unknown opening quest " + opening.asString()); }
            catch (NumberFormatException e) { errors.add(manifestFile + ": invalid opening quest " + opening.asString()); }
        }
        Path enemyFile = resource("world/enemies.json", "enemy catalog");
        if (enemyFile != null) for (JsonValue enemy : json(enemyFile)) {
            String name = enemy.getString("name", "");
            if (!enemies.add(name)) errors.add(enemyFile + ": duplicate enemy " + name);
            JsonValue decks = enemy.get("deck");
            if (decks != null) for (JsonValue deck : decks) checkDeck(resource(deck.asString(), enemyFile + " enemy " + name));
            String sprite = enemy.getString("sprite", "");
            if (!sprite.isBlank()) resource(sprite, enemyFile + " enemy " + name);
            rewards(enemy.get("rewards"), enemyFile + " enemy " + name);
        }
        Path poiFile = resource("world/points_of_interest.json", "location catalog");
        Set<String> names = new HashSet<>();
        if (poiFile != null) for (JsonValue point : json(poiFile)) {
            names.add(point.getString("name", ""));
            String map = point.getString("map", "");
            // Root POI maps deliberately use the common prefix in the runtime.
            Path file = common.resolve(map).normalize();
            if (!file.startsWith(common) || !Files.isRegularFile(file)) errors.add(poiFile + ": missing root map " + map);
            else map(file);
        }
        String start = manifest.getString("startingLocation", "");
        if (start != null && !start.isBlank() && !names.contains(start)) errors.add(manifestFile + ": missing starting location " + start);
        Path configFile = resource("config.json", "configuration");
        if (configFile != null) {
            JsonValue difficulties = json(configFile).get("difficulties");
            if (difficulties != null) for (JsonValue difficulty : difficulties) {
                JsonValue decks = difficulty.get("constructedStarterDecks");
                if (decks != null) for (JsonValue deck : decks) checkDeck(resource(deck.asString(), configFile.toString()));
            }
        }
    }

    private void validateStages(JsonValue quest, String source) {
        JsonValue stages = quest.get("stages");
        if (stages == null) return;
        Map<Integer, Set<Integer>> requirements = new HashMap<>();
        for (JsonValue stage : stages) {
            int id = stage.getInt("id", -1);
            Set<Integer> prerequisites = new HashSet<>();
            JsonValue values = stage.get("prerequisiteIDs");
            if (values != null) for (JsonValue value : values) prerequisites.add(value.asInt());
            if (requirements.put(id, prerequisites) != null) errors.add(source + ": duplicate stage " + id);
            for (String key : new String[]{"prologue", "epilogue", "failureDialog"})
                if (stage.has(key)) dialog(stage.get(key), source + " stage " + id, null, 0);
        }
        Set<Integer> reachable = new HashSet<>();
        boolean changed;
        do {
            changed = false;
            for (var entry : requirements.entrySet())
                if (reachable.containsAll(entry.getValue())) changed |= reachable.add(entry.getKey());
        } while (changed);
        for (var entry : requirements.entrySet()) {
            for (int prerequisite : entry.getValue()) if (!requirements.containsKey(prerequisite))
                errors.add(source + " stage " + entry.getKey() + ": missing prerequisite " + prerequisite);
            if (!reachable.contains(entry.getKey())) errors.add(source + " stage " + entry.getKey() + ": cyclic or unreachable prerequisites");
        }
    }

    private void map(Path file) {
        if (!checked.add(file)) return;
        try {
            var document = xml(file);
            var objects = document.getElementsByTagName("object");
            Set<Integer> actorIds = new HashSet<>();
            for (int i = 0; i < objects.getLength(); i++) actorIds.add(Integer.parseInt(((Element) objects.item(i)).getAttribute("id")));
            for (int i = 0; i < objects.getLength(); i++) {
                Element object = (Element) objects.item(i);
                int id = Integer.parseInt(object.getAttribute("id"));
                String source = file + " object " + id;
                var properties = object.getElementsByTagName("property");
                for (int j = 0; j < properties.getLength(); j++) {
                    Element property = (Element) properties.item(j);
                    String name = property.getAttribute("name");
                    String value = property.hasAttribute("value") ? property.getAttribute("value") : property.getTextContent();
                    if (name.equals("enemy") && !enemies.contains(value)) errors.add(source + ": unknown enemy " + value);
                    if ((name.equals("dialog") || name.equals("defeatDialog")) && !value.isBlank()) {
                        try { for (JsonValue item : new JsonReader().parse(value)) dialog(item, source, actorIds, id); }
                        catch (RuntimeException e) { errors.add(source + ": malformed dialogue: " + e.getMessage()); }
                    }
                    if (name.equals("teleport") && !value.isBlank()) {
                        Path target = resource(value, source);
                        if (target != null) map(target);
                    }
                }
            }
            references(file, document);
        } catch (Exception e) { errors.add(file + ": cannot inspect map: " + e.getMessage()); }
    }

    private org.w3c.dom.Document xml(Path file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        return factory.newDocumentBuilder().parse(file.toFile());
    }

    private void references(Path owner, org.w3c.dom.Document document) throws Exception {
        var nodes = document.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) for (String attribute : new String[]{"source", "template"}) {
            Element node = (Element) nodes.item(i);
            if (!node.hasAttribute(attribute)) continue;
            Path target = owner.getParent().resolve(node.getAttribute(attribute)).normalize();
            if ((!target.startsWith(root) && !target.startsWith(common)) || !Files.isRegularFile(target)) {
                errors.add(owner + ": missing " + attribute + " " + node.getAttribute(attribute));
            } else if ((target.toString().endsWith(".tsx") || target.toString().endsWith(".tx")) && checked.add(target)) {
                references(target, xml(target));
            }
        }
    }

    private void dialog(JsonValue dialog, String source, Set<Integer> actorIds, int parentId) {
        JsonValue actions = dialog.get("action");
        if (actions != null) for (JsonValue action : actions) {
            int actor = action.getInt("battleWithActorID", 0);
            if (actor != 0 && actorIds != null && !actorIds.contains(actor < 0 ? parentId : actor))
                errors.add(source + ": missing battle actor " + actor);
            rewards(action.get("grantRewards"), source);
            rewards(action.get("grantRewardsChoice"), source);
        }
        JsonValue options = dialog.get("options");
        if (!dialog.getString("text", "").isBlank() && (options == null || options.size == 0)) {
            errors.add(source + ": dialogue text needs a dismissible choice; the runtime would hide it immediately");
        }
        if (options != null) for (JsonValue option : options) {
            if (option.getString("name", "").isBlank() && option.getString("locname", "").isBlank()) errors.add(source + ": choice has no visible label");
            dialog(option, source, actorIds, parentId);
        }
    }

    private void rewards(JsonValue rewards, String source) {
        if (rewards == null) return;
        for (JsonValue reward : rewards) {
            String card = reward.getString("cardName", "");
            if (!card.isBlank() && !cardExists.test(card)) errors.add(source + ": unknown reward card " + card);
        }
    }

    private void checkDeck(Path file) {
        if (file == null || !checked.add(file) || !file.toString().endsWith(".dck")) return;
        try {
            for (String line : Files.readAllLines(file)) {
                String value = line.trim();
                if (!value.matches("[0-9]+ .+")) continue;
                String name = value.substring(value.indexOf(' ') + 1).split("\\|", 2)[0].trim();
                if (!cardExists.test(name)) errors.add(file + ": unknown deck card " + name);
            }
        } catch (IOException e) { errors.add(file + ": cannot read deck: " + e.getMessage()); }
    }
}
