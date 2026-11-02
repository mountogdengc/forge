package forge.adventure.editor;

import com.badlogic.gdx.files.FileHandle;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

public class AllowedSetsSelectionTest {
    @Test
    public void filteringDoesNotDropCheckedChoicesAndAllModeRetainsCuratedChoices() {
        AllowedSetsSelection selection = new AllowedSetsSelection(Map.of("LEA", "Limited Edition Alpha", "M10", "Magic 2010"));
        selection.reload("{allowedEditions:[LEA]}");
        assertEquals(selection.visible("mAgIc").get(0).code, "M10");
        selection.setChecked("M10", true);
        assertEquals(selection.visible("lea").size(), 1);
        assertEquals(selection.codesToSave(), List.of("LEA", "M10"));
        selection.setAllSets(true);
        assertTrue(selection.codesToSave().isEmpty());
        selection.setAllSets(false);
        assertEquals(selection.codesToSave(), List.of("LEA", "M10"));
        selection.setChecked("LEA", false);
        assertEquals(selection.codesToSave(), List.of("M10"));
    }

    @Test
    public void reloadReadsLatestLocalConfigAndUnknownCodesSurviveSaving() throws Exception {
        Path root = Files.createTempDirectory("allowed-selection");
        Path config = root.resolve("config.json");
        Path inherited = root.resolve("inherited.json");
        try {
            Files.writeString(inherited, "{allowedEditions:[OLD],restrictedEditions:[BAD],future:42}");
            AllowedSetsSelection selection = new AllowedSetsSelection(Map.of("LEA", "Alpha"));
            selection.reload(EditorFiles.read(root, "config.json", new FileHandle(inherited.toFile())).readString("UTF-8"));
            assertEquals(selection.codesToSave(), List.of("OLD"));
            Files.writeString(config, "{allowedEditions:[UNINSTALLED,LEA],restrictedEditions:[BAD],future:43}");
            selection.reload(EditorFiles.read(root, "config.json", new FileHandle(inherited.toFile())).readString("UTF-8"));
            assertEquals(selection.codesToSave(), List.of("UNINSTALLED", "LEA"));
            assertEquals(selection.visible("uninstalled").size(), 1);
            assertTrue(selection.restrictionSummary().contains("BAD"));
            AllowedSetsStore.save(config, selection.codesToSave());
            selection.reload(Files.readString(config));
            assertEquals(selection.codesToSave(), List.of("UNINSTALLED", "LEA"));
            assertTrue(Files.readString(config).contains("43"));
            assertTrue(selection.visible("OLD").isEmpty());
            assertTrue(Files.readString(inherited).contains("42"));
        } finally {
            Files.deleteIfExists(config);
            Files.deleteIfExists(inherited);
            Files.deleteIfExists(root);
        }
    }

    @Test
    public void curatedModeRequiresAtLeastOneCheckedSet() {
        AllowedSetsSelection selection = new AllowedSetsSelection(Map.of());
        selection.reload("{}");
        assertTrue(selection.isAllSets());
        selection.setAllSets(false);
        assertThrows(IllegalArgumentException.class, selection::codesToSave);
    }
}
