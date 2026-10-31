package forge.adventure.world;

/** Lightweight persistent identity for one New Game, independent of its display name. */
public class PlaythroughMetadata {
    public int formatVersion;
    public String playthroughId;
    public String campaignId;
    public String campaignVersion;
    public String campaignTitle;
    public String plane;
    public String runName;
    public String characterName;
    public long createdAt;

    public PlaythroughMetadata copy() {
        PlaythroughMetadata copy = new PlaythroughMetadata();
        copy.formatVersion = formatVersion;
        copy.playthroughId = playthroughId;
        copy.campaignId = campaignId;
        copy.campaignVersion = campaignVersion;
        copy.campaignTitle = campaignTitle;
        copy.plane = plane;
        copy.runName = runName;
        copy.characterName = characterName;
        copy.createdAt = createdAt;
        return copy;
    }

    public String getPlaythroughId() { return playthroughId; }
    public String getCampaignId() { return campaignId; }
    public String getCampaignVersion() { return campaignVersion; }
    public String getCampaignTitle() { return campaignTitle; }
    public String getRunName() { return runName; }
    public String getCharacterName() { return characterName; }

    @Override
    public String toString() {
        return runName + " — " + campaignTitle + " (" + characterName + ")";
    }
}
