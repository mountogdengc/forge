package forge.app;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class DesktopGraphicsPolicyTest {
    @Test public void normalLauncherHasAnIndependentFrameLimit() throws Exception {
        Object config = create(false);
        assertEquals(value(config, "foregroundFPS"), 60);
        assertEquals(value(config, "vSyncEnabled"), true);
        assertEquals(value(config, "disableAudio"), false);
    }
    @Test public void diagnosticModeLimitsRenderingAndDisablesAudio() throws Exception {
        Object config = create(true);
        assertEquals(value(config, "foregroundFPS"), 30);
        assertEquals(value(config, "disableAudio"), true);
    }
    @Test public void audioComparisonRetainsTheSameRenderingLimits() throws Exception {
        Object config = Class.forName("forge.app.DesktopGraphicsPolicy")
                .getDeclaredMethod("create", boolean.class, boolean.class).invoke(null, true, true);
        assertEquals(value(config, "foregroundFPS"), 30);
        assertEquals(value(config, "idleFPS"), 15);
        assertEquals(value(config, "vSyncEnabled"), true);
        assertEquals(value(config, "disableAudio"), false);
    }
    private static Object create(boolean diagnostic) throws Exception {
        return Class.forName("forge.app.DesktopGraphicsPolicy").getDeclaredMethod("create", boolean.class).invoke(null, diagnostic);
    }
    private static Object value(Object config, String name) throws Exception {
        Class<?> type = Lwjgl3ApplicationConfiguration.class;
        while (type != null) {
            try {
                var field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(config);
            } catch (NoSuchFieldException absent) { type = type.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }
}
