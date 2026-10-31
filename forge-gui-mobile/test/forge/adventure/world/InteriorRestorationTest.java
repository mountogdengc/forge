package forge.adventure.world;

import forge.adventure.character.MapActor;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Set;

import static org.testng.Assert.*;

public class InteriorRestorationTest {
    @Test
    public void anonymousInteractiveActorsBindToTheirTmxIdentityWithoutRebindingOtherObjects() {
        MapActor actor = new MapActor(0);
        actor.bindMapObjectId(82);
        assertEquals(actor.getId(), 82);
        expectThrows(IllegalArgumentException.class, () -> actor.bindMapObjectId(99));
    }
    private static final class ContactProbe extends MapActor {
        int entries;
        ContactProbe() { super(38); }
        @Override public void onPlayerCollide() { entries++; }
        boolean contact(boolean overlapping) { return acceptContact(overlapping); }
    }

    @Test
    public void restoredOverlapDoesNotReplayEntryUntilPlayerLeavesAndReturns() {
        ContactProbe entry = new ContactProbe();
        entry.suppressContactUntilSeparation();
        assertFalse(entry.contact(true));
        assertFalse(entry.contact(true));
        assertEquals(entry.entries, 0);
        assertFalse(entry.contact(false));
        assertTrue(entry.contact(true));
        assertEquals(entry.entries, 1);
        assertTrue(entry.contact(true));
        assertEquals(entry.entries, 1);
    }

    @Test(expectedExceptions = IOException.class)
    public void missingLiveActorRejectsRestoreRatherThanResettingAnEncounter() throws Exception {
        ExplorationSnapshot state = new ExplorationSnapshot();
        ExplorationSnapshot.ActorState actor = new ExplorationSnapshot.ActorState();
        actor.id = 60;
        state.actors.add(actor);
        state.validateActorIds(Set.of());
    }

    @Test(expectedExceptions = IOException.class)
    public void unexpectedActorRejectsRestoreRatherThanResurrectingAnEncounter() throws Exception {
        new ExplorationSnapshot().validateActorIds(Set.of(60));
    }

    @Test(expectedExceptions = IOException.class)
    public void invalidSavedPlayerPositionRejectsRestore() throws Exception {
        ExplorationSnapshot state = new ExplorationSnapshot();
        state.playerX = 481;
        state.validateMapDimensions(480, 272);
    }
}
