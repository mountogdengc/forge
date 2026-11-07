package forge.adventure.data;

import forge.adventure.world.SaveLocator;

import java.util.HashMap;

/**
 * Data class that will be used to read Json configuration files
 * SettingData
 * contains settings outside of the chosen adventure
 */
public class SettingData {

    /** Fill missing resolution settings without replacing a player's explicit choice. */
    public void applyResolutionDefaults(boolean nativeMobile) {
        if (width == 0) width = nativeMobile ? 1280 : 1920;
        if (height == 0) height = nativeMobile ? 720 : 1080;
        if (videomode == null || videomode.isEmpty()) {
            videomode = nativeMobile ? "720p" : "1080p";
        }
    }

	public int width;
    public int height;
    public String plane;
    /** Pending Adventure-only appearance; consumed on a full process restart. */
    public String adventureThemeId;
    public boolean fullScreen;
    public String videomode;
    public String lastActiveSave;
    public SaveLocator lastAdventureSave;
    public Float rewardCardAdj;
    public Float cardTooltipAdj;
    public Float rewardCardAdjLandscape;
    public Float cardTooltipAdjLandscape;
    public boolean dayNightBG;
    public boolean disableWinLose;
    public boolean disableNotForSale;
    public boolean showShopOverlay;
    public boolean useAllCardVariants;
    public boolean disableCrackedItems;
    public boolean excludeAlchemyVariants;
    public boolean generateLDADecks;
    public boolean bindEquipmentLoadoutsToDecks;
    public boolean drawChevronsToHiddenEnemiesInClearQuest;
    public boolean preferEraMatchedTokenArt;
    // Preserve explicitly stored global choices from the original background settings.
    public Boolean enableExtraBattleBackgrounds;
    public HashMap<String, Boolean> extraBattleBackgroundsByPlane = new HashMap<>();
    public HashMap<String, String> battleBackgroundSources = new HashMap<>();
    public HashMap<String, Boolean> useCustomBattleBackgroundSources = new HashMap<>();
}
