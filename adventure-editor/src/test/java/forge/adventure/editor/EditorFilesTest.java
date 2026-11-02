package forge.adventure.editor;

import com.badlogic.gdx.files.FileHandle;
import org.testng.annotations.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.testng.Assert.*;

public class EditorFilesTest {
    @Test public void inheritedReadBecomesLocalOverrideWithoutChangingSharedFile() throws Exception {
        Path parent=Files.createTempDirectory("campaign-editor-files-");
        Path campaign=Files.createDirectory(parent.resolve("chapter"));
        Path common=Files.createDirectories(parent.resolve("common/world"));
        Path shared=common.resolve("items.json"); Files.writeString(shared,"shared items");
        FileHandle cached=new FileHandle(shared.toFile());
        assertEquals(EditorFiles.read(campaign,"world/items.json",cached).readString(),"shared items");
        EditorFiles.writeTarget(campaign,"world/items.json").writeString("authored items",false);
        assertEquals(Files.readString(shared),"shared items");
        assertEquals(EditorFiles.read(campaign,"world/items.json",cached).readString(),"authored items",
                "Reload used the cached inherited file instead of the newly saved override");
    }
    @Test public void editorCannotWriteOutsideItsSelectedCampaign() throws Exception {
        Path root=Files.createTempDirectory("campaign-editor-boundary-");
        assertThrows(IllegalArgumentException.class,() -> EditorFiles.writeTarget(root,"../common/world/items.json"));
        assertThrows(IllegalArgumentException.class,() -> EditorFiles.writeTarget(root,root.resolve("outside.json").toString()));
    }
}
