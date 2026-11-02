package forge.app;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/** A driver VSync override must not leave the desktop client rendering without a limit. */
final class DesktopGraphicsPolicy {
    private DesktopGraphicsPolicy() { }

    static Lwjgl3ApplicationConfiguration create(boolean diagnostic) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setForegroundFPS(diagnostic ? 30 : 60);
        config.setIdleFPS(15);
        config.useVsync(true);
        config.disableAudio(diagnostic);
        System.out.println("Desktop graphics: frame limit=" + (diagnostic ? 30 : 60)
                + ", VSync=true, audio=" + !diagnostic + ", diagnostic=" + diagnostic);
        return config;
    }
}
