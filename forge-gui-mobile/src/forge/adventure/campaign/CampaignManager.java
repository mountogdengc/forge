package forge.adventure.campaign;

import forge.Forge;
import forge.adventure.data.RewardData;
import forge.adventure.data.AdventureQuestData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.player.AdventurePlayerRuntime;
import forge.adventure.scene.*;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.MapStage;
import forge.adventure.stage.WorldStage;
import forge.adventure.util.AdventureEventController;
import forge.adventure.util.AdventureQuestController;
import forge.adventure.util.CardUtil;
import forge.adventure.util.Config;
import forge.adventure.util.JSONStringLoader;
import forge.adventure.world.WorldSave;
import forge.assets.FSkinTexture;
import forge.gui.GuiBase;
import forge.sound.MusicPlaylist;
import forge.sound.SoundSystem;

/** Commits a validated campaign at a new-game/load boundary, never during campaign preview. */
public final class CampaignManager {
    private CampaignManager() { }

    public static CampaignDefinition current() {
        CampaignDefinition selected = Config.instance().getCampaign();
        return selected != null ? selected
                : CampaignRegistry.discover().findByPlane(Config.instance().getSettingData().plane);
    }

    /** Must run on the render thread after the caller has checkpointed the outgoing playthrough. */
    public static void activate(CampaignDefinition campaign) {
        // Resolve and validate everything that is independent of live game state before mutation.
        Config candidate = Config.forCampaign(campaign);

        WorldSave.clearLoadListeners();
        AdventurePlayerRuntime.clearUiListeners(AdventurePlayer.current());
        Forge.clearAdventureSceneHistory();
        resetScenes();
        GameHUD.resetForCampaign();
        MapStage.resetForCampaign();
        WorldStage.resetForCampaign();
        WorldSave.getCurrentSave().getWorld().resetCampaignData();
        AdventureQuestController.clear();
        AdventureEventController.clear();
        AdventureDeckEditor.currentEvent = null;
        JSONStringLoader.clearCache();
        CardUtil.clearPriceCache();
        RewardData.invalidateCardPool();

        Config.activateCampaign(candidate);
        Forge.advFreezePlayerControls = false;
        GuiBase.setAdventureDirectory(candidate.getPrefix());
        GuiBase.setAdventureCacheDirectory(candidate.getCachePrefix());
        Forge.getLocalizer().loadAdventureBundle(candidate.getPrefix() + "languages/");
        FSkinTexture.invalidateAdventureTextures();
        candidate.loadResources();
        // loadResources initializes a pool using the outgoing player mode. Rebuild lazily after restore.
        RewardData.invalidateCardPool();
        SoundSystem.instance.stopBackgroundMusic();
        MusicPlaylist.invalidateMusicPlaylist();
    }

    public static void applyStartingQuests() {
        CampaignDefinition campaign = current();
        if (campaign == null) throw new IllegalStateException("No campaign is active.");
        for (String quest : campaign.getStartingQuestIds()) {
            AdventureQuestData opening = AdventureQuestController.instance().generateQuest(Integer.parseInt(quest));
            if (opening == null) throw new IllegalStateException("Campaign opening quest " + quest + " is unavailable.");
            AdventurePlayer.current().addQuest(opening, true);
        }
    }

    private static void resetScenes() {
        NewGameScene.resetForCampaign();
        SaveLoadScene.resetForCampaign();
        StartScene.resetForCampaign();
        CampaignSelectionScene.resetForCampaign();
        ArenaScene.resetForCampaign();
        InventoryScene.resetForCampaign();
        ViewRewardsScene.resetForCampaign();
        RewardScene.resetForCampaign();
        TileMapScene.resetForCampaign();
        QuestLogScene.resetForCampaign();
        InnScene.resetForCampaign();
        GameScene.resetForCampaign();
        PlayerStatisticScene.resetForCampaign();
        SpellSmithScene.resetForCampaign();
        ShopScene.resetForCampaign();
        EventScene.resetForCampaign();
        ShardTraderScene.resetForCampaign();
        DuelScene.resetForCampaign();
        MapViewScene.resetForCampaign();
        DeckSelectScene.resetForCampaign();
        SettingsScene.resetForCampaign();
        DeckEditScene.resetForCampaign();
        DeckPreviewScene.resetForCampaign();
    }
}
