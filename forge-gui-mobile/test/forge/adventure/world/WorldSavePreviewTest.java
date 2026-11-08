package forge.adventure.world;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import forge.GuiMobile;
import forge.Forge;
import forge.util.Localizer;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/** Exercises real dispatch without touching a graphics buffer or writing a player save. */
@Test(singleThreaded = true)
public class WorldSavePreviewTest {
    private IGuiBase oldGui;
    private Application oldApplication;
    private WorldSaveHeader oldHeader;
    private Object oldSession;
    private boolean oldAvailable, oldRestoring;
    private long oldTimestamp;
    private Boolean oldFirstCapture;
    private Object oldLocalizer, oldForgeLocalizer;
    private final List<Runnable> pending = new ArrayList<>();
    private CountingHeader header;
    private boolean onGuiThread;

    private static final class CountingHeader extends WorldSaveHeader {
        int captures;
        @Override public void createPreview() { captures++; }
    }

    @BeforeMethod
    public void setUp() throws Exception {
        oldGui = GuiBase.getInterface();
        oldApplication = Gdx.app;
        Path resources = Path.of("forge-gui").toAbsolutePath();
        if (!resources.toFile().isDirectory()) resources = Path.of("../forge-gui").toAbsolutePath();
        GuiBase.setInterface(new GuiMobile(resources + "/") {
            @Override public boolean isGuiThread() { return onGuiThread; }
        });
        oldLocalizer = field(Localizer.class, "instance").get(null);
        oldForgeLocalizer = field(Forge.class, "localizer").get(null);
        field(Localizer.class, "instance").set(null, null);
        Localizer.getInstance().initialize("en-US", resources.resolve("res/languages").toString());
        field(Forge.class, "localizer").set(null, Localizer.getInstance());
        Gdx.app = (Application) Proxy.newProxyInstance(Application.class.getClassLoader(),
                new Class<?>[]{Application.class}, (proxy, method, args) -> {
                    if (method.getName().equals("postRunnable")) pending.add((Runnable) args[0]);
                    return null;
                });
        WorldSave save = WorldSave.getCurrentSave();
        oldHeader = save.header;
        oldSession = field("activePlaythrough").get(null);
        oldAvailable = field("worldAvailable").getBoolean(null);
        oldRestoring = field("restoring").getBoolean(null);
        oldTimestamp = field("lastPreviewTimestamp").getLong(null);
        field("activePlaythrough").set(null, new PlaythroughSession(new PlaythroughMetadata()));
        field("worldAvailable").setBoolean(null, true);
        field("restoring").setBoolean(null, false);
        field("lastPreviewTimestamp").setLong(null, 0);
        try {
            oldFirstCapture = field("firstCapture").getBoolean(null);
            field("firstCapture").setBoolean(null, true);
        } catch (NoSuchFieldException ignored) { oldFirstCapture = null; }
        header = new CountingHeader();
        save.header = header;
        onGuiThread = true;
        pending.clear();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        WorldSave.getCurrentSave().header = oldHeader;
        field("activePlaythrough").set(null, oldSession);
        field("worldAvailable").setBoolean(null, oldAvailable);
        field("restoring").setBoolean(null, oldRestoring);
        field("lastPreviewTimestamp").setLong(null, oldTimestamp);
        if (oldFirstCapture != null) field("firstCapture").setBoolean(null, oldFirstCapture);
        Gdx.app = oldApplication;
        GuiBase.setInterface(oldGui);
        field(Localizer.class, "instance").set(null, oldLocalizer);
        field(Forge.class, "localizer").set(null, oldForgeLocalizer);
        pending.clear();
    }

    @Test
    public void acceptedGuiCaptureIsImmediateAndSecondRequestUsesCooldown() {
        WorldSave.requestPreview();
        assertEquals(header.captures, 1);
        WorldSave.requestPreview();
        assertEquals(header.captures, 1);
        assertTrue(pending.isEmpty(), "A graphics-thread capture must not run after drawing the menu");
    }

    @Test
    public void evenTheFirstBackgroundRequestQueuesItsCapture() {
        onGuiThread = false;
        WorldSave.requestPreview();
        assertEquals(header.captures, 0, "A worker cannot read the graphics back buffer");
        assertEquals(pending.size(), 1);
        pending.remove(0).run();
        assertEquals(header.captures, 1);
    }

    @Test
    public void forcedQuicksaveRefreshesPreviewInsideCooldown() throws Exception {
        WorldSave.requestPreview();
        requestForced();
        assertEquals(header.captures, 2, "Quicksave must not serialize the previous preview");
        assertTrue(pending.isEmpty());
    }

    @Test
    public void queuedCaptureCannotTargetAReplacementHeader() throws Exception {
        queueCapture();
        CountingHeader replacement = new CountingHeader();
        WorldSave.getCurrentSave().header = replacement;
        pending.remove(0).run();
        assertEquals(header.captures, 0);
        assertEquals(replacement.captures, 0, "An old scene must not overwrite a newly loaded preview");
    }

    @Test
    public void queuedCaptureCannotCrossPlaythroughActivations() throws Exception {
        queueCapture();
        field("activePlaythrough").set(null, new PlaythroughSession(new PlaythroughMetadata()));
        pending.remove(0).run();
        assertEquals(header.captures, 0, "Loading even the same run invalidates queued capture work");
    }

    @Test
    public void queuedCaptureCannotOverwriteANewerForcedCapture() throws Exception {
        queueCapture();
        onGuiThread = true;
        requestForced();
        assertEquals(header.captures, 1);
        pending.remove(0).run();
        assertEquals(header.captures, 1, "An older queued request must not overwrite the forced world preview");
    }

    private void queueCapture() throws Exception {
        onGuiThread = false;
        // On the old implementation queue a subsequent request, so this specifically exercises stale callbacks.
        if (oldFirstCapture != null) field("firstCapture").setBoolean(null, false);
        WorldSave.requestPreview();
        assertEquals(pending.size(), 1);
    }

    private static void requestForced() throws Exception {
        try {
            WorldSave.class.getMethod("requestPreview", boolean.class).invoke(null, true);
        } catch (NoSuchMethodException missingBeforeImport) {
            WorldSave.requestPreview();
        }
    }

    private static Field field(String name) throws Exception {
        return field(WorldSave.class, name);
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
