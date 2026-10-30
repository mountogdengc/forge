package forge.adventure.world;

/** JSON save envelope. The existing binary game-state/header layout is retained inside the payload. */
public class SaveSnapshot {
    public int formatVersion;
    public String campaignId;
    public String campaignVersion;
    public String campaignTitle;
    public String plane;
    public String playthroughId;
    public String runName;
    public String characterName;
    public String snapshotId;
    public SaveKind kind;
    public String label;
    public String location;
    public long savedAt;
    public String payloadSha256;

    public SaveSnapshot copy() {
        SaveSnapshot copy = new SaveSnapshot();
        copy.formatVersion = formatVersion;
        copy.campaignId = campaignId;
        copy.campaignVersion = campaignVersion;
        copy.campaignTitle = campaignTitle;
        copy.plane = plane;
        copy.playthroughId = playthroughId;
        copy.runName = runName;
        copy.characterName = characterName;
        copy.snapshotId = snapshotId;
        copy.kind = kind;
        copy.label = label;
        copy.location = location;
        copy.savedAt = savedAt;
        copy.payloadSha256 = payloadSha256;
        return copy;
    }

    public String getCampaignId() { return campaignId; }
    public String getCampaignVersion() { return campaignVersion; }
    public String getPlaythroughId() { return playthroughId; }
    public String getSnapshotId() { return snapshotId; }
    public String getLabel() { return label; }
    public String getLocation() { return location; }
    public SaveKind getKind() { return kind; }
    public long getSavedAt() { return savedAt; }

    @Override
    public String toString() {
        return label + " [" + kind + "]";
    }
}
