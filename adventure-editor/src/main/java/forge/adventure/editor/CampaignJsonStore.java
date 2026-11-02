package forge.adventure.editor;
import com.badlogic.gdx.utils.JsonReader;
import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Atomic authored JSON repairs, with stale-write and project-boundary protection. */
final class CampaignJsonStore {
 private CampaignJsonStore() { }
 static void save(Path root,String name,String expected,String text) throws IOException {
  if (!name.endsWith(".json")) throw new IllegalArgumentException("Choose a JSON file.");
  Path base=root.toAbsolutePath().normalize(); Path relative=Path.of(name); Path target=base.resolve(relative).normalize();
  if (relative.isAbsolute() || !target.startsWith(base) || target.equals(base)) throw new IllegalArgumentException("File must be inside this campaign.");
  for (Path part=target;part!=null && part.startsWith(base);part=part.getParent())
   if (Files.isSymbolicLink(part)) throw new IOException("Linked files cannot be edited here.");
  try { new JsonReader().parse(text); }
  catch (RuntimeException error) { throw new IllegalArgumentException("Invalid JSON: " + error.getMessage(),error); }
  String actual=Files.isRegularFile(target)?Files.readString(target,StandardCharsets.UTF_8):null;
  if (!Objects.equals(expected,actual)) throw new IOException("This file changed on disk. Reload before saving to avoid losing changes.");
  Files.createDirectories(target.getParent());
  if (!target.getParent().toRealPath().startsWith(base.toRealPath())) throw new IOException("File leaves the campaign through a link.");
  Path temporary=Files.createTempFile(target.getParent(),".json-edit-",".tmp");
  try {
   Files.writeString(temporary,text,StandardCharsets.UTF_8);
   try { Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
   catch (AtomicMoveNotSupportedException e) { Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING); }
  } finally { Files.deleteIfExists(temporary); }
 }
}
