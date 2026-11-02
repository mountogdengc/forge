package forge.adventure.campaign;

import java.nio.file.Path;
import java.nio.file.Files;

/** Explicit campaign-owned root maps coexist with legacy common map references. */
public final class CampaignMapResolver {
    public static final String PREFIX = "campaign:";
    private CampaignMapResolver() { }
    /** Resource selection prefix is not part of the in-map teleport/save identity. */
    public static String mapIdentity(String reference) {
        return reference.startsWith(PREFIX) ? reference.substring(PREFIX.length()) : reference;
    }
    public static Path resolve(Path campaign,Path common,String reference) {
        if (reference == null || reference.isBlank()) throw new IllegalArgumentException("Location has no map.");
        boolean owned = reference.startsWith(PREFIX);
        String name = owned ? reference.substring(PREFIX.length()) : reference;
        Path base = (owned ? campaign : common).toAbsolutePath().normalize();
        Path relative = Path.of(name);
        Path file = base.resolve(relative).normalize();
        if (relative.isAbsolute() || !file.startsWith(base))
            throw new IllegalArgumentException("Map reference leaves its " + (owned ? "campaign" : "shared") + " folder: " + reference);
        if (!Files.isRegularFile(file)) throw new IllegalArgumentException("Missing root map " + reference);
        try {
            if (!file.toRealPath().startsWith(base.toRealPath()))
                throw new IllegalArgumentException("Map reference leaves its folder through a link: " + reference);
        } catch (java.io.IOException error) {
            throw new IllegalArgumentException("Cannot read root map " + reference, error);
        }
        return file;
    }
}
