package forge.adventure.world;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ExplorationSavePolicyTest {
    @Test
    public void zeroTimerDoesNotMakeAnUnrunRewardCallbackSafeToSave() {
        assertTrue(ExplorationSavePolicy.hasPendingAction(0f, true));
        assertTrue(ExplorationSavePolicy.hasPendingAction(-0.1f, true));
        assertTrue(ExplorationSavePolicy.hasPendingAction(0.1f, false));
        assertFalse(ExplorationSavePolicy.hasPendingAction(0f, false));
    }
    @Test
    public void explorationAndItsMenusCanSave() {
        assertEquals(ExplorationSavePolicy.blockReason(false, false, false, false), "");
    }

    @Test
    public void unfinishedActionsCannotPublishPartialSnapshots() {
        assertFalse(ExplorationSavePolicy.blockReason(true, false, false, false).isEmpty());
        assertFalse(ExplorationSavePolicy.blockReason(false, true, false, false).isEmpty());
        assertFalse(ExplorationSavePolicy.blockReason(false, false, true, false).isEmpty());
        assertFalse(ExplorationSavePolicy.blockReason(false, false, false, true).isEmpty());
    }
}
