package forge.adventure.data;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class StarterModePolicyTest {
    @Test
    public void inheritedCampaignsKeepAllModesWhileCuratedCampaignsCanRestrictThem() {
        ConfigData inherited = new ConfigData();
        assertTrue(inherited.allowsStarterMode("Chaos"));
        ConfigData curated = new ConfigData();
        curated.allowedStarterModes = new String[]{"Constructed"};
        assertTrue(curated.allowsStarterMode("Constructed"));
        assertFalse(curated.allowsStarterMode("Chaos"));
        assertFalse(curated.allowsStarterMode("Custom"));
        assertFalse(curated.allowsStarterMode("Precon"));
    }
}
