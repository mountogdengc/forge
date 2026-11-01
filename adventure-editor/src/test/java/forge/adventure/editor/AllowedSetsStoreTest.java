package forge.adventure.editor;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.*;

public class AllowedSetsStoreTest {
    @Test
    public void selectedSetsRoundTripWithoutChangingOtherFieldsOrCampaigns() throws Exception {
        Path directory = Files.createTempDirectory("adventure-allowed-sets");
        Path selected = directory.resolve("config.json");
        Path unrelated = directory.resolve("other-config.json");
        try {
            Files.writeString(selected, "{allowedEditions:[OLD],restrictedEditions:[BAD],futureSetting:{value:42}}");
            Files.writeString(unrelated, "{allowedEditions:[OTHER]}");
            AllowedSetsStore.save(selected, List.of("LEA", "M10"));
            JsonValue saved = new JsonReader().parse(Files.readString(selected));
            assertEquals(saved.get("allowedEditions").asStringArray(), new String[]{"LEA", "M10"});
            assertEquals(saved.get("restrictedEditions").asStringArray(), new String[]{"BAD"});
            assertEquals(saved.get("futureSetting").getInt("value"), 42);
            assertEquals(Files.readString(unrelated), "{allowedEditions:[OTHER]}");
            AllowedSetsStore.save(selected, List.of());
            assertNull(new JsonReader().parse(Files.readString(selected)).get("allowedEditions"),
                    "Clearing must restore null/unrestricted semantics in all legacy campaign consumers");
        } finally {
            Files.deleteIfExists(selected);
            Files.deleteIfExists(unrelated);
            Files.deleteIfExists(directory);
        }
    }

    @Test
    public void invalidConfigurationIsNeverOverwritten() throws Exception {
        Path config = Files.createTempFile("adventure-invalid-config", ".json");
        try {
            Files.writeString(config, "[not-an-object]");
            assertThrows(IllegalArgumentException.class, () -> AllowedSetsStore.save(config, List.of("LEA")));
            assertEquals(Files.readString(config), "[not-an-object]");
        } finally {
            Files.deleteIfExists(config);
        }
    }
}
