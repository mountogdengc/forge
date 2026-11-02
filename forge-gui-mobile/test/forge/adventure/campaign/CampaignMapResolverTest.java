package forge.adventure.campaign;

import org.testng.annotations.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.testng.Assert.*;

public class CampaignMapResolverTest {
    @Test public void ownedRootAndTeleportHaveOneIdentity() {
        assertEquals(CampaignMapResolver.mapIdentity("campaign:maps/home.tmx"), "maps/home.tmx");
        assertEquals(CampaignMapResolver.mapIdentity("maps/home.tmx"), "maps/home.tmx");
        assertEquals(CampaignMapResolver.mapIdentity("../common/maps/home.tmx"), "../common/maps/home.tmx");
    }
    @Test public void legacyMapsStayCommonEvenWhenCampaignHasSameName() throws Exception {
        Path root=Files.createTempDirectory("campaign-maps-");
        Path campaign=Files.createDirectories(root.resolve("project/maps"));
        Path common=Files.createDirectories(root.resolve("common/maps"));
        Files.writeString(campaign.resolve("home.tmx"),"local"); Files.writeString(common.resolve("home.tmx"),"shared");
        assertEquals(CampaignMapResolver.resolve(campaign.getParent(),common.getParent(),"maps/home.tmx"),common.resolve("home.tmx"));
        assertEquals(CampaignMapResolver.resolve(campaign.getParent(),common.getParent(),"../common/maps/home.tmx"),common.resolve("home.tmx"));
    }
    @Test public void explicitOwnedMapsResolveInsideTheirCampaign() throws Exception {
        Path root=Files.createTempDirectory("owned-maps-");
        Path map=root.resolve("maps/start.tmx"); Files.createDirectories(map.getParent()); Files.writeString(map,"<map/>");
        assertEquals(CampaignMapResolver.resolve(root,root.resolve("common"),"campaign:maps/start.tmx"),map);
    }
    @Test public void missingAndEscapingMapsAreRejected() throws Exception {
        Path root=Files.createTempDirectory("map-boundaries-");
        assertThrows(IllegalArgumentException.class,()->CampaignMapResolver.resolve(root,root.resolve("common"),"campaign:../other.tmx"));
        assertThrows(IllegalArgumentException.class,()->CampaignMapResolver.resolve(root,root.resolve("common"),"campaign:maps/missing.tmx"));
        assertThrows(IllegalArgumentException.class,()->CampaignMapResolver.resolve(root,root.resolve("common"),"../../other.tmx"));
    }
    @Test public void draftMetadataRemainsReadableWhenGameWorldIsIncomplete() throws Exception {
        Path root=Files.createTempDirectory("editable-draft-");
        Files.createDirectories(root.resolve("world"));
        Files.writeString(root.resolve("campaign.json"),"{schemaVersion:1,id:user.draft,version:0.1.0,title:Draft,startingQuestIds:[],futureField:kept}");
        Files.writeString(root.resolve("world/world.json"),"{width:0,height:0,tileSize:16}");
        Files.writeString(root.resolve("world/quests.json"),"[]");
        assertThrows(IllegalArgumentException.class,()->CampaignRegistry.readProject(root,root.resolve("common")));
        assertEquals(CampaignRegistry.readProjectMetadata(root,root.resolve("common")).getId(),"user.draft");
    }
}
