package forge.adventure.world;

/** A Continue target, without dependence on the global plane selection. */
public class SaveLocator {
    public String campaignId;
    public String campaignVersion;
    public String playthroughId;
    public String snapshotId;

    public SaveLocator() { }

    public SaveLocator(SaveSnapshot snapshot) {
        campaignId = snapshot.campaignId;
        campaignVersion = snapshot.campaignVersion;
        playthroughId = snapshot.playthroughId;
        snapshotId = snapshot.snapshotId;
    }
}
