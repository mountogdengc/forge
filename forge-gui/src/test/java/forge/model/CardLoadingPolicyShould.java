package forge.model;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class CardLoadingPolicyShould {
    private boolean lazy(boolean mobile, boolean libgdx, String mode, boolean preference, boolean forceEager) throws Exception {
        return (Boolean) Class.forName("forge.model.CardLoadingPolicy")
                .getMethod("useLazyScripts", boolean.class, boolean.class, String.class, boolean.class, boolean.class)
                .invoke(null, mobile, libgdx, mode, preference, forceEager);
    }

    @Test public void deferDesktopAdventureByDefault() throws Exception {
        assertTrue(lazy(false, true, "Adventure", false, false));
    }
    @Test public void retainOtherPortsAndUserPreferences() throws Exception {
        assertFalse(lazy(false, false, "Adventure", false, false));
        assertFalse(lazy(false, true, "Classic", false, false));
        assertTrue(lazy(false, false, "Classic", true, false));
        assertFalse(lazy(true, true, "Adventure", true, false));
        assertFalse(lazy(false, true, "Adventure", true, true));
        assertFalse(lazy(false, true, null, false, false));
    }
}
