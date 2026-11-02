package forge.adventure.editor;

import org.testng.annotations.Test;
import com.badlogic.gdx.utils.JsonReader;
import java.nio.file.Files;
import static org.testng.Assert.*;

public class CampaignMetadataStoreTest {
    @Test public void editingMetadataPreservesIdentityAndUnknownFields() throws Exception {
        var root=Files.createTempDirectory("campaign-metadata-");
        var file=root.resolve("campaign.json");
        Files.writeString(file,"{schemaVersion:1,id:user.original,version:0.1.0,title:Old,startingQuestIds:[7],future:{value:kept}}");
        CampaignMetadataStore.save(root,"New title","New description","0.2.0");
        var doc=new JsonReader().parse(Files.readString(file));
        assertEquals(doc.getString("id"),"user.original");
        assertEquals(doc.getString("title"),"New title");
        assertEquals(doc.getString("version"),"0.2.0");
        assertEquals(doc.get("future").getString("value"),"kept");
        assertEquals(doc.get("startingQuestIds").get(0).asInt(),7);
    }
    @Test public void invalidMetadataDoesNotChangeTheDescriptor() throws Exception {
        var root=Files.createTempDirectory("campaign-invalid-metadata-");
        var file=root.resolve("campaign.json"); String original="{id:user.original,version:0.1.0,title:Original}";
        Files.writeString(file,original);
        assertThrows(IllegalArgumentException.class,()->CampaignMetadataStore.save(root," ","","0.1.0"));
        assertThrows(IllegalArgumentException.class,()->CampaignMetadataStore.save(root,"Name","","bad version"));
        assertEquals(Files.readString(file),original);
    }
    @Test public void legacyDetailsCanBeEditedWithoutWizardOrIdentityChange() throws Exception {
        var root=Files.createTempDirectory("legacy-details-"); var common=Files.createDirectories(root.resolveSibling(root.getFileName()+"-common"));
        Files.createDirectories(common.resolve("world")); Files.writeString(common.resolve("world/quests.json"),"[]");
        var before=forge.adventure.campaign.CampaignRegistry.readProjectMetadata(root,common,true);
        CampaignMetadataStore.save(before,"Renamed legacy","Description",before.getVersion());
        var after=forge.adventure.campaign.CampaignRegistry.readProjectMetadata(root,common,true);
        assertEquals(after.getId(),before.getId()); assertEquals(after.getVersion(),before.getVersion());
        assertEquals(after.getTitle(),"Renamed legacy");
    }
}
