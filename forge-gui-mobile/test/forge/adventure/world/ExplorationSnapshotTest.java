package forge.adventure.world;

import forge.adventure.util.SaveFileData;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.*;

public class ExplorationSnapshotTest {
    private ExplorationSnapshot interior() {
        ExplorationSnapshot state = new ExplorationSnapshot();
        state.interior = true;
        state.poiId = "bellwake-quay";
        state.map = "../common/maps/map/bellwake/quay.tmx";
        state.sourceMap = "";
        state.playerX = 112.5f;
        state.playerY = 64;
        state.facing = "Right";
        ExplorationSnapshot.ActorState actor = new ExplorationSnapshot.ActorState();
        actor.id = 60;
        actor.x = 80;
        actor.y = 90;
        actor.facing = "Left";
        actor.frozen = true;
        state.actors.add(actor);
        return state;
    }

    @Test
    public void interiorRoundTripRetainsLocationAndDetachedActors() throws Exception {
        ExplorationSnapshot original = interior();
        SaveFileData encoded = original.toSaveData();
        original.actors.get(0).x = 999;
        ExplorationSnapshot restored = ExplorationSnapshot.fromSaveData(encoded);
        assertTrue(restored.interior);
        assertEquals(restored.poiId, "bellwake-quay");
        assertEquals(restored.playerX, 112.5f);
        assertEquals(restored.facing, "Right");
        assertEquals(restored.actors.get(0).x, 80f);
        assertTrue(restored.actors.get(0).frozen);
    }

    @Test
    public void connectedFloorRetainsSourceAndDestination() throws Exception {
        ExplorationSnapshot state = interior();
        state.sourceMap = "maps/map/bellwake/landing.tmx";
        state.map = "maps/map/bellwake/cellar.tmx";
        ExplorationSnapshot restored = ExplorationSnapshot.fromSaveData(state.toSaveData());
        assertEquals(restored.sourceMap, "maps/map/bellwake/landing.tmx");
        assertEquals(restored.map, "maps/map/bellwake/cellar.tmx");
    }

    @Test
    public void interactiveObjectStateSurvivesRoundTrip() throws Exception {
        ExplorationSnapshot state = interior();
        state.actors.get(0).portalAnimation = "active";
        state.actors.get(0).visible = false;
        state.actors.get(0).blocking = false;
        ExplorationSnapshot restored = ExplorationSnapshot.fromSaveData(state.toSaveData());
        assertEquals(restored.actors.get(0).portalAnimation, "active");
        assertFalse(restored.actors.get(0).visible);
        assertFalse(restored.actors.get(0).blocking);
    }

    @Test
    public void perVisitDefeatsRemainAbsentUntilLeavingTheMap() throws Exception {
        ExplorationSnapshot state = interior();
        state.actors.clear();
        state.absentActorIds.add(60);
        ExplorationSnapshot restored = ExplorationSnapshot.fromSaveData(state.toSaveData());
        restored.validateActorIds(java.util.Set.of(60));
        assertEquals(restored.absentActorIds, java.util.Set.of(60));
    }

    @Test
    public void selectedShopAndSeedSurviveIndependentlyOfSubsequentRandomDraws() throws Exception {
        ExplorationSnapshot state = interior();
        state.actors.get(0).shopDataJson = "{name:Curios,restockPrice:4,rewards:[]}";
        state.actors.get(0).shopSeed = 1729L;
        state.actors.get(0).shopStockGenerated = true;
        ExplorationSnapshot restored = ExplorationSnapshot.fromSaveData(state.toSaveData());
        assertEquals(restored.actors.get(0).shopSeed, 1729L);
        assertTrue(restored.actors.get(0).shopDataJson.contains("Curios"));
        assertTrue(restored.actors.get(0).shopStockGenerated);
    }

    @Test
    public void overworldRoundTripDoesNotRequireAnInterior() throws Exception {
        ExplorationSnapshot state = new ExplorationSnapshot();
        assertFalse(ExplorationSnapshot.fromSaveData(state.toSaveData()).interior);
    }

    @Test(expectedExceptions = IOException.class)
    public void interiorWithoutPoiIsRejected() throws Exception {
        ExplorationSnapshot state = interior();
        state.poiId = "";
        state.toSaveData();
    }

    @Test(expectedExceptions = IOException.class)
    public void nonFiniteCoordinatesAreRejected() throws Exception {
        ExplorationSnapshot state = interior();
        state.playerX = Float.NaN;
        state.toSaveData();
    }

    @Test(expectedExceptions = IOException.class)
    public void futureVersionIsRejected() throws Exception {
        ExplorationSnapshot state = interior();
        state.version = 200;
        state.toSaveData();
    }

    @Test(expectedExceptions = IOException.class)
    public void absoluteMapIsRejected() throws Exception {
        ExplorationSnapshot state = interior();
        state.map = "C:/outside/map.tmx";
        state.toSaveData();
    }

    @Test(expectedExceptions = IOException.class)
    public void damagedSectionCannotSilentlyRestoreOverworld() throws Exception {
        ExplorationSnapshot.fromSaveData(new SaveFileData());
    }
}
