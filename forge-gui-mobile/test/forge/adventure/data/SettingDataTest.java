package forge.adventure.data;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class SettingDataTest {
    @Test public void freshDesktopUsesTestedWindowSize() {
        SettingData settings = new SettingData();
        settings.applyResolutionDefaults(false);
        assertEquals(settings.width, 1920);
        assertEquals(settings.height, 1080);
        assertEquals(settings.videomode, "1080p");
        assertFalse(settings.fullScreen);
    }

    @Test public void nativeMobileRetainsLegacyWindowSize() {
        SettingData settings = new SettingData();
        settings.applyResolutionDefaults(true);
        assertEquals(settings.width, 1280);
        assertEquals(settings.height, 720);
        assertEquals(settings.videomode, "720p");
    }

    @Test public void explicitSavedDimensionsSurviveDefaultsOnEitherPlatform() {
        for (boolean mobile : new boolean[] {false, true}) {
            SettingData settings = new SettingData();
            settings.width = 1600;
            settings.height = 900;
            settings.fullScreen = true;
            settings.applyResolutionDefaults(mobile);
            assertEquals(settings.width, 1600);
            assertEquals(settings.height, 900);
            assertTrue(settings.fullScreen);
        }
    }

    @Test public void partiallySavedDimensionsKeepTheExplicitAxis() {
        SettingData settings = new SettingData();
        settings.width = 1440;
        settings.applyResolutionDefaults(false);
        assertEquals(settings.width, 1440);
        assertEquals(settings.height, 1080);
        settings = new SettingData();
        settings.height = 800;
        settings.applyResolutionDefaults(false);
        assertEquals(settings.width, 1920);
        assertEquals(settings.height, 800);
    }

    @Test public void emptyVideoModeReceivesThePlatformDefault() {
        for (boolean mobile : new boolean[] {false, true}) {
            SettingData settings = new SettingData();
            settings.videomode = "";
            settings.applyResolutionDefaults(mobile);
            assertEquals(settings.videomode, mobile ? "720p" : "1080p");
        }
    }

    @Test public void explicitSavedVideoModeSurvivesDefaultsOnEitherPlatform() {
        for (boolean mobile : new boolean[] {false, true}) {
            SettingData settings = new SettingData();
            settings.videomode = "900p";
            settings.applyResolutionDefaults(mobile);
            assertEquals(settings.videomode, "900p");
        }
    }
}
