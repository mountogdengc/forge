package forge.adventure.world;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Campaign-independent save storage. Payloads are immutable; publishing the JSON
 * envelope is the final commit step. An interrupted write cannot replace an older
 * checkpoint or make a partial snapshot visible in the browser.
 */
public final class PlaythroughRepository {
    private static final int FORMAT_VERSION = 1;
    private static final int AUTOMATIC_HISTORY = 3;
    private final Path root;

    @FunctionalInterface
    public interface SnapshotWriter { void write(Path temporaryFile) throws IOException; }
    @FunctionalInterface
    public interface SnapshotValidator { void validate(Path temporaryFile) throws IOException; }

    /** Owns copies of metadata and the destination captured before a background save is queued. */
    public static final class WriteRequest {
        private final Path repositoryRoot;
        private final Path directory;
        private final SaveSnapshot snapshot;

        private WriteRequest(Path root, Path directory, SaveSnapshot snapshot) {
            this.repositoryRoot = root;
            this.directory = directory;
            this.snapshot = snapshot.copy();
        }

        public SaveSnapshot getSnapshot() { return snapshot.copy(); }
    }

    public PlaythroughRepository(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public synchronized PlaythroughMetadata createPlaythrough(String campaignId, String campaignVersion,
            String campaignTitle, String plane, String runName, String characterName) throws IOException {
        requireText(campaignId, "campaign ID");
        requireText(campaignVersion, "campaign version");
        requireText(plane, "campaign resource root");
        PlaythroughMetadata run = new PlaythroughMetadata();
        run.formatVersion = FORMAT_VERSION;
        run.playthroughId = UUID.randomUUID().toString();
        run.campaignId = campaignId;
        run.campaignVersion = campaignVersion;
        run.campaignTitle = fallback(campaignTitle, campaignId);
        run.plane = plane;
        run.characterName = fallback(characterName, "Adventurer");
        run.runName = fallback(runName, run.characterName);
        run.createdAt = System.currentTimeMillis();
        Path directory = runDirectory(run.playthroughId);
        Files.createDirectories(directory.resolve("saves"));
        writeJson(directory.resolve("playthrough.json"), run);
        return run.copy();
    }

    public PlaythroughMetadata readPlaythrough(String playthroughId) throws IOException {
        PlaythroughMetadata run = readJson(runDirectory(playthroughId).resolve("playthrough.json"), PlaythroughMetadata.class);
        validateRun(run, playthroughId);
        return run;
    }

    public List<PlaythroughMetadata> listPlaythroughs() {
        List<PlaythroughMetadata> result = new ArrayList<>();
        Path directory = root.resolve("playthroughs");
        if (!Files.isDirectory(directory)) return result;
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(directory)) {
            for (Path path : paths) {
                if (!Files.isDirectory(path)) continue;
                try { result.add(readPlaythrough(path.getFileName().toString())); }
                catch (IOException e) { System.err.println("Unable to read playthrough " + path + ": " + e.getMessage()); }
            }
        } catch (IOException e) { System.err.println("Unable to list playthroughs: " + e.getMessage()); }
        result.sort(Comparator.comparingLong((PlaythroughMetadata run) -> run.createdAt).reversed());
        return result;
    }

    public List<SaveSnapshot> listSnapshots(String playthroughId) {
        List<SaveSnapshot> result = new ArrayList<>();
        try {
            PlaythroughMetadata run = readPlaythrough(playthroughId);
            Path directory = runDirectory(playthroughId).resolve("saves");
            if (!Files.isDirectory(directory)) return result;
            try (DirectoryStream<Path> paths = Files.newDirectoryStream(directory, "*.json")) {
                for (Path path : paths) {
                    try {
                        SaveSnapshot snapshot = readJson(path, SaveSnapshot.class);
                        validateSnapshot(snapshot, run);
                        if (!path.getFileName().toString().equals(snapshot.snapshotId + ".json")) continue;
                        if (Files.isRegularFile(directory.resolve(snapshot.snapshotId + ".sav"))) result.add(snapshot);
                    } catch (IOException e) { System.err.println("Unable to read save metadata " + path + ": " + e.getMessage()); }
                }
            }
        } catch (IOException e) { System.err.println("Unable to list saves: " + e.getMessage()); }
        result.sort(Comparator.comparingLong((SaveSnapshot snapshot) -> snapshot.savedAt).reversed()
                .thenComparing(snapshot -> snapshot.snapshotId));
        return result;
    }

    public WriteRequest prepareSave(PlaythroughMetadata run, SaveKind kind, String label) throws IOException {
        return prepareSave(run, kind, label, "");
    }

    public WriteRequest prepareSave(PlaythroughMetadata run, SaveKind kind, String label, String location) throws IOException {
        PlaythroughMetadata stored = readPlaythrough(run.playthroughId);
        if (!stored.campaignId.equals(run.campaignId) || !stored.campaignVersion.equals(run.campaignVersion)) {
            throw new IOException("Playthrough campaign identity does not match its stored metadata.");
        }
        if (kind == null) throw new IOException("A save kind is required.");
        SaveSnapshot snapshot = new SaveSnapshot();
        snapshot.formatVersion = FORMAT_VERSION;
        snapshot.campaignId = stored.campaignId;
        snapshot.campaignVersion = stored.campaignVersion;
        snapshot.campaignTitle = stored.campaignTitle;
        snapshot.plane = stored.plane;
        snapshot.playthroughId = stored.playthroughId;
        snapshot.runName = stored.runName;
        snapshot.characterName = stored.characterName;
        snapshot.snapshotId = UUID.randomUUID().toString();
        snapshot.kind = kind;
        snapshot.label = fallback(label, kind.toString());
        snapshot.location = location == null ? "" : location;
        snapshot.savedAt = System.currentTimeMillis();
        return new WriteRequest(root, runDirectory(stored.playthroughId).resolve("saves"), snapshot);
    }

    public synchronized SaveSnapshot writeSnapshot(WriteRequest request, SnapshotWriter writer,
            SnapshotValidator validator) throws IOException {
        if (!root.equals(request.repositoryRoot)) throw new IOException("Save destination belongs to another repository.");
        Files.createDirectories(request.directory);
        SaveSnapshot snapshot = request.snapshot.copy();
        Path payload = request.directory.resolve(snapshot.snapshotId + ".sav");
        Path envelope = request.directory.resolve(snapshot.snapshotId + ".json");
        if (Files.exists(payload) || Files.exists(envelope)) throw new IOException("Snapshot already exists.");
        Path temporary = Files.createTempFile(request.directory, "save-", ".tmp");
        boolean published = false;
        try {
            writer.write(temporary);
            validator.validate(temporary);
            // Durability belongs to the repository so imported copies and every
            // future payload writer receive the same guarantee as game saves.
            try (FileChannel file = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                file.force(true);
            }
            snapshot.payloadSha256 = digest(temporary);
            moveCompleted(temporary, payload);
            writeJson(envelope, snapshot);
            published = true;
        } finally {
            Files.deleteIfExists(temporary);
            if (!published) {
                Files.deleteIfExists(envelope);
                Files.deleteIfExists(payload);
            }
        }
        // Retention only touches committed automatic snapshots in this run. Named
        // manual checkpoints and the initial/imported recovery point are retained.
        if (snapshot.kind == SaveKind.AUTO || snapshot.kind == SaveKind.QUICK) pruneAutomatic(snapshot);
        return snapshot.copy();
    }

    public SaveSnapshot resolve(SaveLocator locator) throws IOException {
        if (locator == null) throw new IOException("No save has been selected.");
        requireUuid(locator.playthroughId);
        requireUuid(locator.snapshotId);
        PlaythroughMetadata run = readPlaythrough(locator.playthroughId);
        SaveSnapshot snapshot = readJson(runDirectory(locator.playthroughId).resolve("saves")
                .resolve(locator.snapshotId + ".json"), SaveSnapshot.class);
        validateSnapshot(snapshot, run);
        if (!locator.snapshotId.equals(snapshot.snapshotId)
                || !snapshot.campaignId.equals(locator.campaignId)
                || !snapshot.campaignVersion.equals(locator.campaignVersion)) {
            throw new IOException("The selected save does not match its campaign/playthrough identity.");
        }
        if (!Files.isRegularFile(payloadPath(snapshot))) throw new IOException("The save payload is missing.");
        return snapshot;
    }

    public Path validatedPayload(SaveSnapshot selection) throws IOException {
        SaveSnapshot stored = resolve(new SaveLocator(selection));
        Path payload = payloadPath(stored);
        if (!stored.payloadSha256.equals(digest(payload))) throw new IOException("The save payload is damaged (checksum mismatch).");
        return payload;
    }

    public synchronized void setContinue(SaveSnapshot snapshot) throws IOException {
        SaveSnapshot stored = resolve(new SaveLocator(snapshot));
        writeJson(root.resolve("continue.json"), new SaveLocator(stored));
    }

    public SaveSnapshot getContinue() throws IOException {
        Path pointer = root.resolve("continue.json");
        return Files.isRegularFile(pointer) ? resolve(readJson(pointer, SaveLocator.class)) : null;
    }

    public synchronized void clearContinue() throws IOException {
        Files.deleteIfExists(root.resolve("continue.json"));
    }

    /** Explicit manual replacement hides the old checkpoint but retains its payload for recovery. */
    public synchronized void archiveManual(SaveSnapshot selection) throws IOException {
        SaveSnapshot stored = resolve(new SaveLocator(selection));
        if (stored.kind != SaveKind.MANUAL) throw new IOException("Only manual checkpoints can be replaced.");
        Path directory = runDirectory(stored.playthroughId).resolve("saves");
        moveCompleted(directory.resolve(stored.snapshotId + ".json"),
                directory.resolve(stored.snapshotId + ".json.replaced"));
    }

    private void pruneAutomatic(SaveSnapshot committed) {
        int retained = 0;
        SaveSnapshot continueTarget = null;
        try { continueTarget = getContinue(); }
        catch (IOException ignored) { /* A damaged locator must not prevent saving. */ }
        for (SaveSnapshot previous : listSnapshots(committed.playthroughId)) {
            if (previous.kind != committed.kind) continue;
            if (++retained <= AUTOMATIC_HISTORY) continue;
            if (previous.snapshotId.equals(committed.snapshotId)) continue;
            // A background write is committed before its render-thread callback
            // updates Continue. Keep that recovery target through this boundary.
            if (continueTarget != null && continueTarget.snapshotId.equals(previous.snapshotId)) continue;
            try {
                // Remove the listing first; a crash may leave an unlisted payload,
                // never an envelope referencing a half-written replacement.
                Path directory = runDirectory(previous.playthroughId).resolve("saves");
                Files.deleteIfExists(directory.resolve(previous.snapshotId + ".json"));
                Files.deleteIfExists(directory.resolve(previous.snapshotId + ".sav"));
            } catch (IOException e) { System.err.println("Unable to prune automatic save: " + e.getMessage()); }
        }
    }

    private Path payloadPath(SaveSnapshot snapshot) throws IOException {
        requireUuid(snapshot.snapshotId);
        return runDirectory(snapshot.playthroughId).resolve("saves").resolve(snapshot.snapshotId + ".sav");
    }

    private Path runDirectory(String id) throws IOException {
        requireUuid(id);
        return root.resolve("playthroughs").resolve(id);
    }

    private static void validateRun(PlaythroughMetadata run, String id) throws IOException {
        if (run == null || run.formatVersion != FORMAT_VERSION || !id.equals(run.playthroughId)) {
            throw new IOException("Unsupported or mismatched playthrough metadata.");
        }
        requireUuid(run.playthroughId);
        requireText(run.campaignId, "campaign ID");
        requireText(run.campaignVersion, "campaign version");
        requireText(run.plane, "campaign resource root");
    }

    private static void validateSnapshot(SaveSnapshot snapshot, PlaythroughMetadata run) throws IOException {
        if (snapshot == null || snapshot.formatVersion != FORMAT_VERSION || snapshot.kind == null
                || !run.playthroughId.equals(snapshot.playthroughId)
                || !run.campaignId.equals(snapshot.campaignId)
                || !run.campaignVersion.equals(snapshot.campaignVersion)
                || !run.plane.equals(snapshot.plane)) throw new IOException("Unsupported or mismatched snapshot metadata.");
        requireUuid(snapshot.snapshotId);
        if (snapshot.payloadSha256 == null || !snapshot.payloadSha256.matches("[a-f0-9]{64}")) {
            throw new IOException("Snapshot integrity metadata is missing.");
        }
    }

    private static void requireUuid(String value) throws IOException {
        try {
            if (value == null || !UUID.fromString(value).toString().equals(value)) throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) { throw new IOException("Invalid save identity."); }
    }

    private static void requireText(String value, String field) throws IOException {
        if (value == null || value.trim().isEmpty()) throw new IOException("Missing " + field + ".");
    }

    private static String fallback(String value, String alternative) {
        return value == null || value.trim().isEmpty() ? alternative : value.trim();
    }

    private static Json json() {
        Json json = new Json(JsonWriter.OutputType.json);
        json.setIgnoreUnknownFields(true);
        json.setUsePrototypes(false);
        return json;
    }

    private static <T> T readJson(Path path, Class<T> type) throws IOException {
        try { return json().fromJson(type, new String(Files.readAllBytes(path), StandardCharsets.UTF_8)); }
        catch (RuntimeException e) { throw new IOException("Invalid metadata: " + path.getFileName(), e); }
    }

    private static void writeJson(Path path, Object value) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "metadata-", ".tmp");
        try {
            byte[] bytes = json().prettyPrint(value).getBytes(StandardCharsets.UTF_8);
            try (FileOutputStream output = new FileOutputStream(temporary.toFile())) {
                output.write(bytes);
                output.getFD().sync();
            }
            moveCompleted(temporary, path);
        } finally { Files.deleteIfExists(temporary); }
    }

    private static void moveCompleted(Path source, Path destination) throws IOException {
        try { Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException e) { Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING); }
    }

    private static String digest(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
            }
            StringBuilder hex = new StringBuilder(64);
            for (byte value : digest.digest()) hex.append(String.format("%02x", value & 0xff));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
