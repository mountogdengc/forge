package forge.adventure.stage;

import org.testng.Assert;
import org.testng.annotations.Test;

public class PointOfInterestEntryCooldownTest {
    @Test
    public void leavingLocationProtectsAgainstReentryForOneAndAHalfSeconds() {
        PointOfInterestEntryCooldown cooldown = new PointOfInterestEntryCooldown();
        Assert.assertFalse(cooldown.isActive());
        cooldown.start();
        Assert.assertTrue(cooldown.isActive());
        cooldown.update(1f);
        Assert.assertTrue(cooldown.isActive());
        cooldown.update(0.5f);
        Assert.assertFalse(cooldown.isActive());
    }

    @Test
    public void anotherExitRestartsTheGracePeriod() {
        PointOfInterestEntryCooldown cooldown = new PointOfInterestEntryCooldown();
        cooldown.start();
        cooldown.update(1f);
        cooldown.start();
        cooldown.update(1f);
        Assert.assertTrue(cooldown.isActive());
        cooldown.update(2f);
        Assert.assertFalse(cooldown.isActive());
    }
}
