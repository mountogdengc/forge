package forge.adventure.data;

import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.util.AdventureQuestController;
import forge.util.Localizer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static forge.adventure.util.AdventureQuestController.ObjectiveTypes.*;
import static forge.adventure.util.AdventureQuestController.QuestStatus.*;
import static org.testng.Assert.*;

public class AdventureQuestProgressionTest {
    @BeforeClass
    public void initializeTextOnlyDependencies() {
        Path languages = Path.of("../forge-gui/res/languages");
        if (!Files.isDirectory(languages)) {
            languages = Path.of("forge-gui/res/languages");
        }
        Localizer.getInstance().initialize("en-US", languages.toAbsolutePath().toString());
    }

    @Test
    public void earlyCastleDefeatWaitsForVisitAndDoesNotSkipTheReturnTrip() throws Exception {
        AdventureQuestStage visit = stage(1, Travel, null, 0);
        AdventureQuestStage rescue = stage(2, QuestFlag, "Ch1GreenCastleComplete", 0, 1);
        AdventureQuestStage returnTrip = stage(3, Travel, null, 0, 2);
        AdventureQuestData quest = quest(visit, rescue, returnTrip);
        Map<String, Integer> flags = Map.of("Ch1GreenCastleComplete", 1);

        reconcile(quest, flags);
        assertEquals(visit.getStatus(), ACTIVE);
        assertEquals(rescue.getStatus(), INACTIVE, "Stored evidence must not bypass a prerequisite.");
        assertEquals(returnTrip.getStatus(), INACTIVE);

        savedStatus(visit, COMPLETE); // The player has now performed the real travel objective.
        reconcile(quest, flags);
        assertEquals(rescue.getStatus(), COMPLETE);
        assertEquals(returnTrip.getStatus(), ACTIVE, "Reconciliation must not synthesize a travel event.");
        assertFalse(quest.completed);
    }

    @Test
    public void missingFlagNeverSatisfiesTheDefaultZeroThreshold() {
        AdventureQuestStage castle = stage(1, QuestFlag, "Ch1GreenCastleComplete", 0);
        AdventureQuestData quest = quest(castle);
        reconcile(quest, Collections.emptyMap());
        assertEquals(castle.getStatus(), ACTIVE);
        assertFalse(quest.completed);

        reconcile(quest, Map.of("Ch1GreenCastleComplete", 1));
        assertTrue(quest.completed);
    }

    @Test
    public void aStoredFlagMustMeetItsThreshold() {
        AdventureQuestStage clues = stage(1, MapFlag, "foundClues", 2);
        AdventureQuestData quest = quest(clues);
        reconcile(quest, Map.of("foundClues", 1));
        assertEquals(clues.getStatus(), ACTIVE);
        reconcile(quest, Map.of("foundClues", 2));
        assertEquals(clues.getStatus(), COMPLETE);
    }

    @Test
    public void chainedFlagsResolveRegardlessOfArrayOrderAndDoNotCompleteTwice() {
        AdventureQuestStage first = stage(1, QuestFlag, "first", 1);
        AdventureQuestStage second = stage(2, QuestFlag, "second", 1, 1);
        AdventureQuestData quest = quest(second, first);
        Map<String, Integer> flags = Map.of("first", 1, "second", 1);
        AtomicInteger completions = new AtomicInteger();

        for (int i = 0; i < 2; i++) {
            AdventureQuestProgression.activateNextStages(quest, s -> {
                boolean completed = s.reconcilePersistentFlag(flags.get(s.mapFlag));
                if (completed) {
                    completions.incrementAndGet();
                }
                return completed;
            });
        }
        assertTrue(quest.completed);
        assertEquals(completions.get(), 2, "Each stage must transition to complete only once.");
    }

    @Test
    public void cyclicPrerequisitesRemainInactive() {
        AdventureQuestStage first = stage(1, QuestFlag, "first", 1, 2);
        AdventureQuestStage second = stage(2, QuestFlag, "second", 1, 1);
        AdventureQuestData quest = quest(first, second);
        reconcile(quest, Map.of("first", 1, "second", 1));
        assertEquals(first.getStatus(), INACTIVE);
        assertEquals(second.getStatus(), INACTIVE);
        assertFalse(quest.completed);
    }

    @Test
    public void alreadyActiveFetchObjectivesKeepTheirEventDrivenBehavior() {
        AdventureQuestStage fetch = stage(1, Fetch, null, 0);
        fetch.checkPrerequisites(Collections.emptyList());
        AdventureQuestData quest = quest(fetch);
        AtomicInteger calls = new AtomicInteger();
        AdventureQuestProgression.activateNextStages(quest, s -> {
            calls.incrementAndGet();
            return false;
        });
        assertEquals(calls.get(), 0);
        assertEquals(fetch.getStatus(), ACTIVE);
    }

    @Test
    public void savedFloorFlagsCannotComeFromAnotherPoiOrPrefixCollision() {
        String target = "123Frostbitten Cavern/../common/maps/cavern_0.tmx";
        PointOfInterestChanges.Map maps = new PointOfInterestChanges.Map();
        addFlag(maps, "999Other Cavern/../common/maps/cavern_0.tmx", "hasAFlower", 1);
        addFlag(maps, target + "-other/../common/maps/cavern_1.tmx", "hasAFlower", 1);
        assertNull(maps.getMapFlag(target, "hasAFlower"));

        addFlag(maps, target + "../common/maps/cavern_1.tmx", "hasAFlower", 1);
        assertEquals(maps.getMapFlag(target, "hasAFlower"), Integer.valueOf(1));
        assertNull(maps.getMapFlag(target, "missingFlag"));

        addFlag(maps, target + "maps/map/other_floor.tmx", "foundClues", 2);
        assertEquals(maps.getMapFlag(target, "foundClues"), Integer.valueOf(2));
        addFlag(maps, target, "rootFlag", 1);
        assertEquals(maps.getMapFlag(target, "rootFlag"), Integer.valueOf(1));
    }

    @Test
    public void acceptedFlowerQuestRecoversAfterSaveRoundTripWithoutReplayingDialogs() throws Exception {
        String target = "123Frostbitten Cavern/../common/maps/cavern_0.tmx";
        PointOfInterestChanges.Map maps = new PointOfInterestChanges.Map();
        addFlag(maps, target + "../common/maps/cavern_1.tmx", "hasAFlower", 1);
        PointOfInterestChanges.Map restoredMaps = new PointOfInterestChanges.Map();
        restoredMaps.load(maps.save());

        AdventureQuestStage flower = stage(1, MapFlag, "hasAFlower", 1);
        flower.checkPrerequisites(Collections.emptyList());
        flower.prologueDisplayed = true;
        flower.epilogueDisplayed = true;
        AdventureQuestData original = quest(flower);
        original.prologue = new DialogData();
        original.epilogue = new DialogData();
        assertNotNull(original.getPrologue());

        AdventureQuestData restored = roundTrip(original);
        for (int i = 0; i < 2; i++) {
            AdventureQuestProgression.activateNextStages(restored,
                    s -> s.reconcilePersistentFlag(restoredMaps.getMapFlag(target, s.mapFlag)));
        }
        assertTrue(restored.completed);
        assertTrue(restored.stages[0].prologueDisplayed);
        assertTrue(restored.stages[0].epilogueDisplayed);
        assertNull(restored.getPrologue(), "A displayed prologue must remain displayed after recovery.");
        assertNotNull(restored.getEpilogue(), "Recovery leaves the pending epilogue for the normal lifecycle.");
        assertNull(restored.getEpilogue(), "The normal lifecycle must deliver the quest epilogue only once.");

        AdventureQuestData loadedAgain = roundTrip(restored);
        assertFalse(loadedAgain.completed, "Aggregate completion is deliberately transient in the save format.");
        reconcile(loadedAgain, Collections.emptyMap());
        assertTrue(loadedAgain.completed);
        assertNull(loadedAgain.getEpilogue(), "Loading completed progress must not repeat its epilogue.");
    }

    @Test
    public void dataActivationUpdatesCompletionForAnAlreadySatisfiedActiveStage() {
        AdventureQuestStage stage = new AdventureQuestStage() {
            @Override
            public boolean reconcilePersistentState() {
                return reconcilePersistentFlag(1);
            }
        };
        stage.id = 1;
        stage.objective = QuestFlag;
        stage.mapFlag = "Ch1GreenCastleComplete";
        stage.checkPrerequisites(Collections.emptyList());
        AdventureQuestData quest = quest(stage);
        quest.activateNextStages();
        assertTrue(quest.completed, "The real activation entry point must update aggregate state.");
        quest.activateNextStages();
        assertTrue(quest.completed);
    }

    @Test
    public void questSerializationIdentifiersRemainCompatibleWithTheForkBaseline() {
        assertEquals(ObjectStreamClass.lookup(AdventureQuestData.class).getSerialVersionUID(), -859190699184917225L);
        assertEquals(ObjectStreamClass.lookup(AdventureQuestStage.class).getSerialVersionUID(), 12042023L);
        assertEquals(ObjectStreamClass.lookup(AdventureQuestController.class).getSerialVersionUID(), 9037330116470199350L);
    }

    private static AdventureQuestStage stage(int id, AdventureQuestController.ObjectiveTypes objective,
                                              String flag, int threshold, Integer... prerequisites) {
        AdventureQuestStage stage = new AdventureQuestStage();
        stage.id = id;
        stage.objective = objective;
        stage.mapFlag = flag;
        stage.mapFlagValue = threshold;
        stage.prerequisiteIDs = Arrays.asList(prerequisites);
        return stage;
    }

    private static AdventureQuestData quest(AdventureQuestStage... stages) {
        AdventureQuestData quest = new AdventureQuestData();
        quest.stages = stages;
        return quest;
    }

    private static void reconcile(AdventureQuestData quest, Map<String, Integer> flags) {
        AdventureQuestProgression.activateNextStages(quest,
                s -> s.reconcilePersistentFlag(s.mapFlag == null ? null : flags.get(s.mapFlag)));
    }

    private static void addFlag(PointOfInterestChanges.Map maps, String key, String flag, int value) {
        maps.computeIfAbsent(key, unused -> new PointOfInterestChanges()).getMapFlags().put(flag, (byte) value);
    }

    private static void savedStatus(AdventureQuestStage stage, AdventureQuestController.QuestStatus status) throws Exception {
        Field field = AdventureQuestStage.class.getDeclaredField("status");
        field.setAccessible(true);
        field.set(stage, status);
    }

    private static AdventureQuestData roundTrip(AdventureQuestData quest) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(quest);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (AdventureQuestData) in.readObject();
        }
    }
}
