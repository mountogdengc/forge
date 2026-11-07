package forge.adventure.util;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import forge.adventure.data.AdventureQuestData;
import forge.adventure.data.AdventureQuestStage;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.util.Localizer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;
import static forge.adventure.util.AdventureQuestController.QuestStatus.*;

public class QuestNavigationTest {
    private Path resources;

    @BeforeClass public void text() {
        Path root = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(root.resolve("forge-gui/res/adventure"))) root = root.getParent();
        resources = root.resolve("forge-gui/res/adventure/CurseOfStrahd");
        Localizer.getInstance().initialize("en-US", root.resolve("forge-gui/res/languages").toString());
    }

    private AdventureQuestData[] quests() throws Exception {
        return new Json().fromJson(AdventureQuestData[].class,
                Files.readString(resources.resolve("world/quests.json")));
    }

    private void activateOnly(AdventureQuestData quest, int index) throws Exception {
        Field status = AdventureQuestStage.class.getDeclaredField("status");
        status.setAccessible(true);
        for (int i = 0; i < quest.stages.length; i++)
            status.set(quest.stages[i], i == index ? ACTIVE : i < index ? COMPLETE : INACTIVE);
    }

    @Test public void chapterOneRoutesToItsCurrentObjectiveInsteadOfTheNearestTown() throws Exception {
        AdventureQuestData quest = quests()[0];
        String[] expected = {"Spawn", "Village Church", "Village Church", "Spawn", "Old Svalich Road", "Tser Pool"};
        for (int i = 0; i < expected.length; i++) {
            activateOnly(quest, i);
            PointOfInterest destination = new PointOfInterest();
            Map<String, PointOfInterest> locations = Map.of(expected[i], destination);
            assertSame(QuestNavigation.destination(quest, null, locations::get, Vector2.Zero), destination,
                    "Wrong destination for " + quest.stages[i].name);
        }
    }

    @Test public void existingSaveUsesCurrentTemplateWithoutChangingQuestProgress() throws Exception {
        AdventureQuestData template = quests()[0];
        AdventureQuestData saved = quests()[0];
        for (AdventureQuestStage stage : saved.stages) stage.navigationTarget = null;
        activateOnly(saved, 5);
        PointOfInterest pool = new PointOfInterest();
        assertSame(QuestNavigation.destination(saved, template, Map.of("Tser Pool", pool)::get, Vector2.Zero), pool);
        assertEquals(saved.stages[4].getStatus(), COMPLETE);
        assertEquals(saved.stages[5].getStatus(), ACTIVE);
        assertFalse(saved.completed);
    }

    @Test public void missingAuthoredDestinationDoesNotFallBackToAnUnrelatedTown() throws Exception {
        AdventureQuestData quest = quests()[0];
        activateOnly(quest, 5);
        quest.stages[5].setTargetPOI(new PointOfInterest());
        assertNull(QuestNavigation.destination(quest, null, name -> null, Vector2.Zero));
    }

    @Test public void generatedQuestsKeepTheirExistingLocationGuidance() throws Exception {
        AdventureQuestData quest = new AdventureQuestData();
        AdventureQuestStage stage = new AdventureQuestStage();
        stage.objective = AdventureQuestController.ObjectiveTypes.Travel;
        PointOfInterest target = new PointOfInterest();
        stage.setTargetPOI(target);
        quest.stages = new AdventureQuestStage[]{stage};
        activateOnly(quest, 0);
        assertSame(QuestNavigation.destination(quest, null, name -> null, Vector2.Zero), target);
        activateOnly(quest, -1);
        assertNull(QuestNavigation.destination(quest, null, name -> null, Vector2.Zero));
    }

    @Test public void copiesKeepTheAuthoredNavigationAndEveryStoryStepHasARealDestination() throws Exception {
        var pois = new Json().fromJson(forge.adventure.data.PointOfInterestData[].class,
                Files.readString(resources.resolve("world/points_of_interest.json")));
        List<String> names = java.util.Arrays.stream(pois).map(p -> p.name).toList();
        for (AdventureQuestData quest : quests()) for (AdventureQuestStage stage : quest.stages) {
            assertTrue(names.contains(stage.navigationTarget), "Missing destination for " + stage.name);
            assertEquals(new AdventureQuestStage(stage).navigationTarget, stage.navigationTarget);
        }
    }
}
