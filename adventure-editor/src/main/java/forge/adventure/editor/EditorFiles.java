package forge.adventure.editor;

import com.badlogic.gdx.files.FileHandle;
import forge.adventure.util.Config;
import java.nio.file.Path;

/** Editor reads may inherit; writes always belong to the selected campaign. */
final class EditorFiles {
    private EditorFiles() { }
    static FileHandle read(String name) { return read(Config.instance(),name); }
    static FileHandle read(Config config,String name) {
        return read(Path.of(config.getPrefix()),name,config.getFile(name));
    }
    static FileHandle read(Path root,String name,FileHandle inherited) {
        FileHandle local = new FileHandle(relative(root,name).toFile());
        return local.exists() ? local : inherited;
    }
    static FileHandle writeTarget(String name) { return writeTarget(Path.of(Config.instance().getPrefix()),name); }
    static FileHandle writeTarget(Path root,String name) {
        FileHandle local = new FileHandle(relative(root,name).toFile());
        local.parent().mkdirs();
        return local;
    }
    private static Path relative(Path root,String name) {
        Path base = root.toAbsolutePath().normalize();
        Path relative = Path.of(name);
        Path target = base.resolve(relative).normalize();
        if (relative.isAbsolute() || !target.startsWith(base) || target.equals(base))
            throw new IllegalArgumentException("Editor files must stay inside the selected campaign: " + name);
        return target;
    }
}
