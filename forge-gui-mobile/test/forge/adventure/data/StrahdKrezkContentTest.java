package forge.adventure.data;

import com.badlogic.gdx.utils.Json;
import forge.adventure.util.AdventureQuestController;
import forge.adventure.campaign.CampaignRegistry;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

public class StrahdKrezkContentTest {
    private Path root() {
        Path path = Path.of("").toAbsolutePath();
        while (path != null && !Files.isDirectory(path.resolve("forge-gui/res/adventure"))) path = path.getParent();
        if (path == null) throw new AssertionError("Missing Adventure resources");
        return path.resolve("forge-gui/res/adventure/CurseOfStrahd");
    }

    @Test public void nineStagesReconcileAcrossSaveBoundariesWithoutArrivalCompletion() throws Exception {
        forge.util.Localizer.getInstance().initialize("en-US", root().getParent().getParent().resolve("languages").toString());
        AdventureQuestData[] quests = new Json().fromJson(AdventureQuestData[].class,
                Files.readString(root().resolve("world/quests.json")));
        assertEquals(quests.length, 3);
        AdventureQuestData quest = new AdventureQuestData(quests[2]);
        assertEquals(quest.getID(), 8103);
        assertTrue(quest.autoTrack);
        assertEquals(quest.stages.length, 9);
        Map<String, Integer> evidence = new HashMap<>();
        for (int i = 0; i < quest.stages.length; i++) {
            AdventureQuestProgression.activateNextStages(quest, stage -> stage.reconcilePersistentFlag(evidence.get(stage.mapFlag)));
            assertFalse(quest.completed, "Entering a scene must not complete the next objective");
            assertEquals(quest.stages[i].getStatus(), AdventureQuestController.QuestStatus.ACTIVE);
            assertEquals(quest.stages[i].objective, AdventureQuestController.ObjectiveTypes.QuestFlag);
            evidence.put(quest.stages[i].mapFlag, 1);
        }
        AdventureQuestProgression.activateNextStages(quest, stage -> stage.reconcilePersistentFlag(evidence.get(stage.mapFlag)));
        assertTrue(quest.completed);
    }

    @Test public void newRosterRejectsPriorSavedWorldVersions() {
        var registry = CampaignRegistry.discover(root().getParent(), root().resolve("absent-user-root"), false);
        assertNotNull(registry.find("fork.curse-of-strahd", "0.5.0"));
        for (String version : new String[]{"0.4.0", "0.3.0", "0.2.0", "0.1.0"}) {
            assertNull(registry.find("fork.curse-of-strahd", version), "Never load an older roster as the expanded world");
        }
    }
}
