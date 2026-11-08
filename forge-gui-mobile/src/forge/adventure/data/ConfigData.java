package forge.adventure.data;

import com.badlogic.gdx.utils.ObjectMap;

/**
 * Data class that will be used to read Json configuration files
 * BiomeData
 * contains general information about the game
 */
public class ConfigData {
    public int screenWidth;
    public int screenHeight;
    public String skin;
    public String font;
    public String fontColor;
    public int minDeckSize;
    public int maxNumberOfDecks;
    public float playerBaseSpeed;
    public String[] colorIds;
    public String[] colorIdNames;
    // Null preserves inherited campaign options; explicit campaigns can curate their starts.
    public String[] allowedStarterModes;
    public CharacterCreationData characterCreation;
    public String chapterCompletionQuestFlag;
    public String chapterCompletionText;

    public boolean allowsStarterMode(String mode) {
        if (allowedStarterModes == null) return true;
        for (String allowed : allowedStarterModes) if (mode.equals(allowed)) return true;
        return false;
    }
    public String[] starterEditions;
    public String[] starterEditionNames;
    public ObjectMap<String, ObjectMap<String, String>> starterDecksByEdition;
    public DifficultyData[] difficulties;
    public RewardData legalCards;
    public String[] restrictedCards;
    public String[] restrictedEditions;
    public String[] restrictedBlocks;
    public String[] restrictedTokens;
    public String[] allowedEditions;
    public boolean vintageOnlyEditions = false;
    public String[] restrictedEvents;
    public String[] allowedEvents;
    public String[] allowedJumpstart;
    public String defaultBasicLandSet = "JMP";
    public boolean enableGeneticAI = true;
    public String chaosDeckFormat;
    public boolean usePriceListPrices = true;
    // Desktop default only; saved player choices take precedence on every platform.
    public boolean enableExtraBattleBackgroundsByDefault;

}
