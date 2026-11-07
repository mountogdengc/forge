package forge.adventure.data;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.testng.Assert.*;

public class StrahdLandmarkContentTest {
    private Path root() {
        Path path = Path.of("").toAbsolutePath();
        while (path != null && !Files.isDirectory(path.resolve("forge-gui/res/adventure"))) path = path.getParent();
        if (path == null) throw new AssertionError("Missing Adventure resources");
        return path.resolve("forge-gui/res/adventure/CurseOfStrahd");
    }
    private JsonValue read(String name) throws Exception { return new JsonReader().parse(Files.readString(root().resolve(name))); }
    private void noEffects(JsonValue node) {
        assertTrue(node.get("action") == null || node.get("action").size == 0, "Landmark interaction must not change state");
        if (node.get("options") != null) for (JsonValue option : node.get("options")) noEffects(option);
    }

    @Test public void referenceSitesAreNewRunOnlyWithTheOriginalChaptersRetained() throws Exception {
        assertEquals(read("campaign.json").getString("version"), "0.5.0");
        assertEquals(read("world/points_of_interest.json").size, 37);
        assertEquals(read("world/quests.json").size, 3);
        JsonValue landmarks = read("story/landmarks.json"); assertEquals(landmarks.size, 29);
        Set<String> names = new HashSet<>();
        for (JsonValue landmark : landmarks) assertTrue(names.add(landmark.getString("name")), "Duplicate landmark");
        assertTrue(names.containsAll(Set.of("Castle Ravenloft", "Krezk", "Old Bonegrinder", "Argynvostholt",
                "Wizard of Wines", "Amber Temple", "Werewolf Den", "Van Richten's Tower")));
    }

    @Test public void arrivalsAreDismissibleAndNeverAdvanceQuestsOrGrantRewards() throws Exception {
        JsonValue story = read("story/dialogues.json");
        for (JsonValue landmark : read("story/landmarks.json")) for (String kind : new String[]{"arrival", "detail"}) {
            if (landmark.getBoolean("questArea", false)) continue;
            JsonValue entry = story.get("landmark." + landmark.getString("slug") + "." + kind);
            assertNotNull(entry, landmark.getString("name"));
            assertEquals(entry.get("dialog").size, 1);
            JsonValue dialog = entry.get("dialog").get(0); noEffects(dialog);
            assertTrue(dialog.get("options").size > 0);
            assertTrue(dialog.getString("text").length() > 30);
            if (kind.equals("arrival")) {
                assertTrue(dialog.getString("text").contains("future chapter"));
                assertTrue(dialog.getString("text").contains("southern"));
            }
        }
    }
}
