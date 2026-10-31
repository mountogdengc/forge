package forge.adventure.data;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static forge.adventure.util.AdventureQuestController.QuestStatus.ACTIVE;
import static forge.adventure.util.AdventureQuestController.QuestStatus.INACTIVE;
import static org.testng.Assert.*;

public class AdventureProgressionContentTest {
    @DataProvider(name = "drunkenPleaDefinitions")
    public Object[][] drunkenPleaDefinitions() {
        return new Object[][] {
            {"Shandalar/world/quests.json"},
            {"Shandalar Old Border/world/quests.json"},
            {"Realm of Legends/world/quests.json"},
            {"Crystal_Kingdoms/world/quests.json"},
            {"Innistrad/world/quests.json"},
            {"Shandalar/languages/quests-es-ES.json"}
        };
    }

    @Test(dataProvider = "drunkenPleaDefinitions")
    public void drunkenPleaRequiresClearingCampBeforeReturning(String file) throws Exception {
        JsonValue quests = new JsonReader().parse(Files.readString(asset(file)));
        JsonValue quest = withId(quests, 37);
        JsonValue stages = quest.get("stages");
        Json json = new Json();
        AdventureQuestStage clear = json.readValue(AdventureQuestStage.class, withId(stages, 1));
        AdventureQuestStage travel = json.readValue(AdventureQuestStage.class, withId(stages, 2));

        assertEquals(clear.objective.name(), "Clear", file);
        assertEquals(travel.objective.name(), "Travel", file);
        assertTrue(travel.here, "Return target must remain the quest's originating town: " + file);
        clear.checkPrerequisites(Collections.emptyList());
        travel.checkPrerequisites(Collections.emptyList());
        assertEquals(clear.getStatus(), ACTIVE, file);
        assertEquals(travel.getStatus(), INACTIVE, "Accepting in town must not satisfy the return leg: " + file);
        travel.checkPrerequisites(Collections.singletonList(1));
        assertEquals(travel.getStatus(), ACTIVE, "Clearing the camp must unlock the return leg: " + file);
    }

    @DataProvider(name = "varsilMaps")
    public Object[][] varsilMaps() {
        return new Object[][] {
            {"common/maps/map/main_story_explore/library_of_varsil_4.tmx"},
            {"Shandalar Old Border/maps/map/main_story_explore/library_of_varsil_4.tmx"}
        };
    }

    @Test(dataProvider = "varsilMaps")
    public void varsilBossDefeatHasOneSafeDialogForEveryStoryState(String file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        NodeList objects = factory.newDocumentBuilder().parse(asset(file).toFile()).getElementsByTagName("object");
        Set<Integer> bossIds = new java.util.HashSet<>();
        for (int i = 0; i < objects.getLength(); i++) {
            Element object = (Element) objects.item(i);
            if (!"Myr Superion".equals(property(object, "enemy"))) {
                continue;
            }
            int objectId = Integer.parseInt(object.getAttribute("id"));
            bossIds.add(objectId);
            String defeatDialog = property(object, "defeatDialog");
            assertNotNull(defeatDialog, file + " object " + objectId);
            JsonValue roots = new JsonReader().parse(defeatDialog);
            for (Integer storyState : new Integer[] {null, 0, 3, 4, 5, 8}) {
                for (boolean hasResearch : new boolean[] {false, true}) {
                    for (boolean alreadyDefeated : new boolean[] {false, true}) {
                        Map<String, Integer> flags = new HashMap<>();
                        if (storyState != null) {
                            flags.put("exploreShand1", storyState);
                        }
                        if (hasResearch) {
                            flags.put("foundLibraryOfVarsilResearch", 1);
                        }
                        if (alreadyDefeated) {
                            flags.put("defeatedLibraryOfVarsilBoss", 1);
                        }
                        String context = file + " object " + objectId + " flags " + flags;
                        JsonValue selected = null;
                        int matches = 0;
                        for (JsonValue root : roots) {
                            if (matchesConditions(root.get("condition"), flags)) {
                                selected = root;
                                matches++;
                            }
                        }
                        // MapDialog.activate processes every matching root, not just the first one.
                        assertEquals(matches, 1, "Defeat dialog must be exhaustive and exclusive: " + context);
                        assertDefeatActions(selected, context);
                        String text = selected.getString("text");
                        if (Integer.valueOf(4).equals(storyState)) {
                            assertTrue(text.contains(hasResearch ? "already found" : "still haven't found"), context);
                        } else {
                            assertFalse(text.contains("Sir Donovan"), "Do not show an inactive story objective: " + context);
                        }
                    }
                }
            }
        }
        assertEquals(bossIds, Set.of(27, 28, 29), "Check every difficulty-specific boss: " + file);
    }

    private static void assertDefeatActions(JsonValue dialog, String context) {
        assertNull(dialog.get("action"), context);
        JsonValue options = dialog.get("options");
        assertNotNull(options, context);
        assertEquals(options.size, 1, context);
        JsonValue continuation = options.get(0);
        assertNull(continuation.get("condition"), context);
        assertNull(continuation.get("options"), context);
        JsonValue actions = continuation.get("action");
        assertNotNull(actions, context);
        int defeats = 0;
        int removals = 0;
        for (JsonValue action : actions) {
            for (JsonValue effect : action) {
                if ("setQuestFlag".equals(effect.name)) {
                    assertEquals(effect.getString("key"), "defeatedLibraryOfVarsilBoss", context);
                    assertEquals(effect.getInt("val"), 1, context);
                    defeats++;
                } else if ("deleteMapObject".equals(effect.name)) {
                    assertEquals(effect.asInt(), -1, context);
                    removals++;
                } else {
                    fail("Boss dialog must not grant extra rewards or advance the story: " + effect.name + " " + context);
                }
            }
        }
        assertEquals(defeats, 1, context);
        assertEquals(removals, 1, "The defeated boss must not remain available for repeated loot: " + context);
    }

    private static boolean matchesConditions(JsonValue conditions, Map<String, Integer> flags) {
        if (conditions == null) {
            return true;
        }
        for (JsonValue condition : conditions) {
            for (JsonValue field : condition) {
                assertTrue(Set.of("getQuestFlag", "checkQuestFlag", "not").contains(field.name),
                        "Unexpected Varsil condition requires an explicit test case: " + field.name);
            }
            boolean matches;
            JsonValue query = condition.get("getQuestFlag");
            if (query != null) {
                String key = query.getString("key");
                // MapDialog rejects an absent numeric flag before applying the condition's negation.
                if (!flags.containsKey(key)) {
                    return false;
                }
                assertEquals(query.getString("op"), "=");
                matches = flags.get(key) == query.getInt("val");
            } else {
                matches = flags.containsKey(condition.getString("checkQuestFlag"));
            }
            if (matches == condition.getBoolean("not", false)) {
                return false;
            }
        }
        return true;
    }

    private static JsonValue withId(JsonValue entries, int id) {
        for (JsonValue entry : entries) {
            if (entry.getInt("id", -1) == id) {
                return entry;
            }
        }
        throw new AssertionError("Missing quest or stage " + id);
    }

    private static String property(Element object, String name) {
        NodeList properties = object.getElementsByTagName("property");
        for (int i = 0; i < properties.getLength(); i++) {
            Element property = (Element) properties.item(i);
            if (name.equals(property.getAttribute("name"))) {
                return property.hasAttribute("value") ? property.getAttribute("value") : property.getTextContent();
            }
        }
        return null;
    }

    private static Path asset(String relativePath) {
        Path base = Path.of("../forge-gui/res/adventure");
        if (!Files.isDirectory(base)) {
            base = Path.of("forge-gui/res/adventure");
        }
        return base.resolve(relativePath);
    }
}
