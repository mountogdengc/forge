package forge.app;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/** A driver VSync override must not leave the desktop client rendering without a limit. */
final class DesktopGraphicsPolicy {
    private DesktopGraphicsPolicy() { }

    static Lwjgl3ApplicationConfiguration create(boolean diagnostic) {
        return create(diagnostic, false);
    }

    static Lwjgl3ApplicationConfiguration create(boolean diagnostic, boolean diagnosticAudio) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setForegroundFPS(diagnostic ? 30 : 60);
        config.setIdleFPS(15);
        config.useVsync(true);
        boolean audio = !diagnostic || diagnosticAudio;
        config.disableAudio(!audio);
        System.out.println("Desktop graphics: frame limit=" + (diagnostic ? 30 : 60)
                + ", VSync=true, audio=" + audio + ", diagnostic=" + diagnostic);
        return config;
    }
}
