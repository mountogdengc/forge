package forge.app;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Clipboard;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import com.google.gson.Gson;
import forge.Forge;
import forge.StaticData;
import forge.adventure.scene.StartScene;
import forge.adventure.campaign.CampaignManager;
import forge.localinstance.properties.ForgeConstants;
import org.lwjgl.glfw.GLFW;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Hidden native startup only: no input, campaign creation, or player profile access. */
public final class StartupBenchmark implements ApplicationListener {
    private static final AtomicBoolean finished = new AtomicBoolean();
    private static long started;
    private static Path output;
    private static Path isolatedRoot;
    private final ApplicationListener game;
    private long windowHandle;
    private long readyFrame = -1;
    private long databaseReadyMs = -1;

    private StartupBenchmark(ApplicationListener game) { this.game = game; }

    public static void main(String[] args) {
        started = System.nanoTime();
        try {
            if (args.length != 3 || !Boolean.getBoolean("forge.startup.isolated"))
                throw new IllegalArgumentException("Use run_startup_benchmark.ps1 with assets, output JSON, and isolated working directory.");
            Path assets = Path.of(args[0]).toAbsolutePath().normalize();
            output = Path.of(args[1]).toAbsolutePath().normalize();
            isolatedRoot = Path.of(args[2]).toAbsolutePath().normalize();
            require(Path.of("").toAbsolutePath().normalize().equals(isolatedRoot), "Working directory is not isolated");
            require(Files.isDirectory(assets.resolve("res")), "Missing resources");
            Thread watchdog = new Thread(() -> {
                try { Thread.sleep(Integer.getInteger("forge.startup.timeoutSeconds", 180) * 1000L); }
                catch (InterruptedException ignored) { return; }
                fail(new AssertionError("Startup timed out"));
            }, "startup-benchmark-watchdog");
            watchdog.setDaemon(true);
            watchdog.start();
            Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
            config.setTitle("Forge hidden startup benchmark");
            config.setWindowedMode(1280, 720);
            config.setInitialVisible(false);
            config.setForegroundFPS(60);
            config.setIdleFPS(60);
            config.setPauseWhenLostFocus(false);
            config.setPauseWhenMinimized(false);
            config.disableAudio(true);
            Main.DesktopAdapter adapter = new Main.DesktopAdapter(isolatedRoot.resolve("switch_orientation.ini").toString()) {
                @Override public boolean isConnectedToInternet() { return false; }
                @Override public boolean isConnectedToWifi() { return false; }
                @Override public void setMouseConfined(boolean ignored) { }
                @Override public void preventSystemSleep(boolean ignored) { }
                @Override public String getLatestChanges(String url, java.util.Date build, java.util.Date max) { return ""; }
                @Override public String getReleaseTag(String url) { return ""; }
                @Override public boolean openFile(String filename) { throw new IllegalStateException("External app launch forbidden"); }
                @Override public void restart() { throw new IllegalStateException("Restart forbidden"); }
            };
            ApplicationListener app = Forge.getApp(null, new Lwjgl3Clipboard(), adapter,
                    assets.toString().replace('\\', '/') + "/", false, false, 0);
            Forge.selector = "Adventure";
            StartupBenchmark benchmark = new StartupBenchmark(app);
            config.setWindowListener(new Lwjgl3WindowAdapter() {
                @Override public void created(Lwjgl3Window window) {
                    benchmark.windowHandle = window.getWindowHandle();
                    benchmark.assertHidden();
                }
            });
            new Lwjgl3Application(benchmark, config);
            require(finished.get(), "Application exited before StartScene was ready");
        } catch (Throwable failure) { fail(failure); }
    }

    @Override public void create() {
        try {
            game.create();
            Thread.setDefaultUncaughtExceptionHandler((thread, failure) -> fail(failure));
            require(Path.of(ForgeConstants.USER_DIR).toAbsolutePath().normalize().equals(isolatedRoot.resolve("test-profile")), "Unexpected player profile");
            require(Path.of(ForgeConstants.CACHE_DIR).toAbsolutePath().normalize().equals(isolatedRoot.resolve("test-cache")), "Unexpected cache directory");
        } catch (Throwable failure) { fail(failure); }
    }

    @Override public void render() {
        try {
            assertHidden();
            game.render();
            if (Forge.afterDBloaded && databaseReadyMs < 0) databaseReadyMs = elapsed();
            if (Forge.getCurrentScene() instanceof StartScene && Forge.getTransitionScreen() == null && Forge.afterDBloaded) {
                if (readyFrame < 0) readyFrame = Gdx.graphics.getFrameId();
                // Prove the actual menu survives a full additional rendered frame.
                if (Gdx.graphics.getFrameId() > readyFrame) finish();
            }
        } catch (Throwable failure) { fail(failure); }
    }

    private void assertHidden() {
        require(windowHandle != 0, "Native window was not created");
        require(GLFW.glfwGetWindowAttrib(windowHandle, GLFW.GLFW_VISIBLE) == GLFW.GLFW_FALSE, "Benchmark window became visible");
        require(GLFW.glfwGetWindowAttrib(windowHandle, GLFW.GLFW_FOCUSED) == GLFW.GLFW_FALSE, "Benchmark acquired focus");
    }

    private void finish() throws Exception {
        require(CampaignManager.current() != null && "fork.bellwake".equals(CampaignManager.current().getId()), "StartScene is not Bellwake");
        if (Boolean.getBoolean("forge.startup.requireDeferred"))
            require(fieldSize(StaticData.instance().getCommonCards(), "rulesByPrimaryName") < 500, "Startup materialized the full catalog");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "ready");
        result.put("main_to_start_scene_ms", elapsed());
        result.put("main_to_after_database_ms", databaseReadyMs);
        result.put("java", System.getProperty("java.version"));
        result.put("max_heap_mb", Runtime.getRuntime().maxMemory() / 1048576);
        result.put("available_processors", Runtime.getRuntime().availableProcessors());
        result.put("rules_loaded", fieldSize(StaticData.instance().getCommonCards(), "rulesByPrimaryName"));
        result.put("printings_loaded", fieldSize(StaticData.instance().getCommonCards(), "allCardsByRules"));
        var readerField = StaticData.class.getDeclaredField("cardReader");
        readerField.setAccessible(true);
        Object reader = readerField.get(StaticData.instance());
        try {
            result.put("index_cache_status", reader.getClass().getMethod("getNameIndexCacheStatus").invoke(reader));
            result.put("index_scripts_parsed", reader.getClass().getMethod("getNameIndexParseCount").invoke(reader));
        } catch (NoSuchMethodException olderBuild) {
            result.put("index_cache_status", "unsupported");
        }
        result.put("used_heap_mb", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / 1048576);
        result.put("peak_heap_pool_mb", ManagementFactory.getMemoryPoolMXBeans().stream().filter(p -> p.getType() == MemoryType.HEAP)
                .mapToLong(p -> p.getPeakUsage().getUsed()).sum() / 1048576);
        result.put("hidden", true);
        result.put("focused", false);
        result.put("audio_enabled", false);
        result.put("campaign", CampaignManager.current().getId());
        result.put("profile", ForgeConstants.USER_DIR);
        result.put("cache", ForgeConstants.CACHE_DIR);
        write(result);
        finished.set(true);
        System.out.flush();
        System.err.flush();
        Runtime.getRuntime().halt(0);
    }

    private static int fieldSize(Object owner, String name) throws Exception {
        var field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        Object value = field.get(owner);
        if (value instanceof Map<?, ?> map) return map.size();
        if (value instanceof com.google.common.collect.Multimap<?, ?> map) return map.size();
        throw new IllegalStateException("Unknown map " + name);
    }

    private static long elapsed() { return (System.nanoTime() - started) / 1_000_000; }
    private static void require(boolean ok, String message) { if (!ok) throw new IllegalStateException(message); }
    private static void write(Map<String, Object> result) throws Exception {
        String json = new Gson().toJson(result);
        Files.writeString(output, json + "\n");
        System.out.println("STARTUP_BENCHMARK " + json);
    }
    private static void fail(Throwable failure) {
        if (finished.getAndSet(true)) return;
        failure.printStackTrace();
        try { if (output != null) write(Map.of("status", "failed", "elapsed_ms", elapsed(), "error", failure.toString())); }
        catch (Throwable writingFailure) { writingFailure.printStackTrace(); }
        System.out.flush();
        System.err.flush();
        Runtime.getRuntime().halt(2);
    }

    @Override public void resize(int width, int height) { game.resize(width, height); }
    @Override public void pause() { game.pause(); }
    @Override public void resume() { game.resume(); }
    @Override public void dispose() { game.dispose(); }
}
