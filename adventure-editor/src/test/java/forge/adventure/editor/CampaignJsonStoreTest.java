package forge.adventure.editor;
import java.nio.file.*;
import org.testng.annotations.Test;
import static org.testng.Assert.*;
public class CampaignJsonStoreTest {
 @Test public void repairsMalformedJsonPreservesAuthoredTextAndRejectsStaleWrites() throws Exception {
  Path root=Files.createTempDirectory("campaign-json-"); Path file=root.resolve("world/quests.json"); Files.createDirectories(file.getParent());
  Files.writeString(file,"{broken:");
  CampaignJsonStore.save(root,"world/quests.json","{broken:","[{id:1,future:kept}]");
  assertEquals(Files.readString(file),"[{id:1,future:kept}]");
  assertThrows(IllegalArgumentException.class,()->CampaignJsonStore.save(root,"world/quests.json","[{id:1,future:kept}]","{broken:"));
  assertEquals(Files.readString(file),"[{id:1,future:kept}]");
  assertThrows(java.io.IOException.class,()->CampaignJsonStore.save(root,"world/quests.json","stale","[]"));
  assertThrows(IllegalArgumentException.class,()->CampaignJsonStore.save(root,"../other.json",null,"{}"));
 }
}
