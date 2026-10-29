package forge.adventure.world;

/** A source slot offered for explicit, non-destructive import as a separate playthrough. */
public class LegacySave {
    public String campaignId;
    public String campaignVersion;
    public String campaignTitle;
    public String plane;
    public String fileName;
    public long modifiedAt;

    @Override
    public String toString() {
        return campaignTitle + " — " + fileName;
    }
}
