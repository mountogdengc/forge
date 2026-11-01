package forge;

import forge.util.Lang;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Small real scripts exercise the persistent index without a full card database. */
public class CardStorageReaderIndexCacheTest {
    @Test
    public void warmIndexPreservesFacesAndArchivePrecedenceWithoutParsing() throws Exception {
        Path root = Files.createTempDirectory("card-index-faces");
        Path cache = root.resolve("cache");
        write(root.resolve("loose.txt"), script("Shared Name"));
        write(root.resolve("split.txt"), script("First Face") + "AlternateMode:Split\nALTERNATE\n" + script("Other Face"));
        write(root.resolve("prepare.txt"), script("Prepared Scholar") + "AlternateMode:Prepare\nALTERNATE\nCopyFaceFrom:Borrowed Spell\n");
        archive(root, "archive.txt", script("Shared Name"));
        CardStorageReader cold = reader(root, cache);
        check("archive.txt".equals(cold.attemptToLoadCard("Shared Name").getPath()), "archive must win over loose card");
        CardStorageReader warm = reader(root, cache);
        check("hit".equals(status(warm)), "second reader must use persisted index");
        check(parseCount(warm) == 0, "warm index must not parse card scripts");
        check(warm.attemptToLoadCard("Other Face") != null, "alternate face missing");
        check(warm.attemptToLoadCard("First Face // Other Face") != null, "split display name missing");
        check(warm.attemptToLoadCard("Borrowed Spell") != null, "placeholder alias missing");
        check(warm.attemptToLoadCard("missing") == null, "unknown alias loaded a card");
    }

    @Test
    public void sourceChangesAndCorruptionRebuildInsteadOfUsingStaleAliases() throws Exception {
        Path root = Files.createTempDirectory("card-index-stale");
        Path cache = root.resolve("cache");
        Path source = root.resolve("card.txt");
        write(source, script("Old Name"));
        reader(root, cache);
        FileTime originalTime = Files.getLastModifiedTime(source);
        write(source, script("New Name With Different Size"));
        Files.setLastModifiedTime(source, originalTime);
        CardStorageReader changed = reader(root, cache);
        check(changed.attemptToLoadCard("Old Name") == null, "old alias survived source change");
        check(changed.attemptToLoadCard("New Name With Different Size") != null, "new alias not indexed");
        try (var files = Files.list(cache)) {
            for (Path file : files.toList()) Files.write(file, new byte[]{1, 2, 3});
        }
        CardStorageReader corrupt = reader(root, cache);
        check(corrupt.attemptToLoadCard("New Name With Different Size") != null, "corrupt cache prevented fallback");
        check(parseCount(corrupt) > 0, "corrupt cache was trusted");
        Files.delete(source);
        write(root.resolve("added.txt"), script("Added Card"));
        CardStorageReader replaced = reader(root, cache);
        check(replaced.attemptToLoadCard("New Name With Different Size") == null, "deleted source alias survived");
        check(replaced.attemptToLoadCard("Added Card") != null, "added source absent");
    }

    @Test
    public void archiveCrcInvalidatesIndexEvenWithIdenticalSizeAndTimestamp() throws Exception {
        Path root = Files.createTempDirectory("card-index-archive");
        Path cache = root.resolve("cache");
        archive(root, "card.txt", script("Old Name"));
        Path zip = root.resolve("cardsfolder.zip");
        FileTime timestamp = Files.getLastModifiedTime(zip);
        long size = Files.size(zip);
        closeArchive(reader(root, cache));
        archive(root, "card.txt", script("New Name"));
        check(Files.size(zip) == size, "test fixture must have equal archive sizes");
        Files.setLastModifiedTime(zip, timestamp);
        CardStorageReader changed = reader(root, cache);
        check(changed.attemptToLoadCard("Old Name") == null, "CRC change did not invalidate index");
        check(changed.attemptToLoadCard("New Name") != null, "changed archive alias absent");
    }

    @Test
    public void unavailableCacheFallsBackAndLegacyConstructorKeepsWorking() throws Exception {
        Path root = Files.createTempDirectory("card-index-unavailable");
        write(root.resolve("card.txt"), script("Visible Card"));
        Path blocked = root.resolve("cache-is-file");
        write(blocked, "not a directory");
        check(reader(root, blocked).attemptToLoadCard("Visible Card") != null, "unwritable cache broke card loading");
        check(new CardStorageReader(root.toString(), null, true).attemptToLoadCard("Visible Card") != null, "legacy constructor changed behavior");
    }

    @Test
    public void failedScriptIsRetriedAfterRecoveryWithUnchangedInventory() throws Exception {
        Path root = Files.createTempDirectory("card-index-incomplete");
        Path cache = root.resolve("cache");
        Path recovering = root.resolve("recovering.txt");
        write(root.resolve("visible.txt"), script("Visible Card"));
        String valid = script("Recoverable Card") + "AlternateMode:None\n";
        String invalid = script("Recoverable Card") + "AlternateMode:Fail\n";
        check(valid.length() == invalid.length(), "recovery fixture must preserve size");
        write(recovering, invalid);
        FileTime timestamp = Files.getLastModifiedTime(recovering);
        CardStorageReader incomplete = reader(root, cache);
        check(incomplete.attemptToLoadCard("Visible Card") != null, "one failed source prevented valid card lookup");
        check(incomplete.attemptToLoadCard("Recoverable Card") == null, "malformed fixture must fail indexing");
        write(recovering, valid);
        Files.setLastModifiedTime(recovering, timestamp);
        CardStorageReader recovered = reader(root, cache);
        check(recovered.attemptToLoadCard("Recoverable Card") != null,
                "persisted partial index hid a recovered script with unchanged inventory");
        check("incomplete".equals(status(incomplete)), "failed index build was reported as complete");
        CardStorageReader warm = reader(root, cache);
        check("hit".equals(status(warm)) && parseCount(warm) == 0,
                "fully recovered index was not persisted");
    }

    @Test
    public void timestampAndRootChangesInvalidateOtherwiseIdenticalInventory() throws Exception {
        Path root = Files.createTempDirectory("card-index-time");
        Path cache = root.resolve("cache");
        Path source = root.resolve("card.txt");
        write(source, script("Old Name"));
        reader(root, cache);
        FileTime time = Files.getLastModifiedTime(source);
        write(source, script("New Name"));
        Files.setLastModifiedTime(source, FileTime.fromMillis(time.toMillis() + 2000));
        CardStorageReader changed = reader(root, cache);
        check(changed.attemptToLoadCard("Old Name") == null, "timestamp-only change was ignored");
        check(changed.attemptToLoadCard("New Name") != null, "timestamp-only change lost new name");
        Path other = Files.createTempDirectory("card-index-other-root");
        write(other.resolve("other.txt"), script("Other Root"));
        CardStorageReader relocated = reader(other, cache);
        check(relocated.attemptToLoadCard("New Name") == null, "another root reused stale aliases");
        check(relocated.attemptToLoadCard("Other Root") != null, "another root was not indexed");
    }

    @Test
    public void wellChecksummedInvalidOrdinalsCountsAndSchemaAreRejected() throws Exception {
        Path cache = Files.createTempDirectory("card-index-invalid");
        byte[] fingerprint = new byte[32];
        for (int scenario = 0; scenario < 4; scenario++) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                out.writeInt(0x46434e49);
                out.writeInt(scenario == 0 ? 999 : 1);
                out.write(fingerprint);
                out.writeInt(0); // no archive aliases
                out.writeInt(scenario == 1 ? Integer.MAX_VALUE : 1);
                out.writeUTF(scenario == 2 ? "../../outside" : "visible_card");
                out.writeInt(scenario == 3 ? 1 : 0); // only ordinal zero exists
            }
            byte[] payload = bytes.toByteArray();
            try (var out = Files.newOutputStream(cache.resolve("lazy-card-name-index.bin"))) {
                out.write(payload);
                out.write(MessageDigest.getInstance("SHA-256").digest(payload));
            }
            check(CardNameIndexCache.read(cache, fingerprint, 0, 1) == null,
                    "invalid cache accepted in scenario " + scenario);
        }
    }

    private static CardStorageReader reader(Path root, Path cache) throws Exception {
        Lang.createInstance("en-US");
        try {
            return CardStorageReader.class.getConstructor(String.class, CardStorageReader.ProgressObserver.class, boolean.class, Path.class)
                    .newInstance(root.toString(), null, true, cache);
        } catch (NoSuchMethodException absentBeforeImplementation) {
            return new CardStorageReader(root.toString(), null, true);
        }
    }

    private static String status(CardStorageReader reader) throws Exception {
        try { return (String) CardStorageReader.class.getMethod("getNameIndexCacheStatus").invoke(reader); }
        catch (NoSuchMethodException absentBeforeImplementation) { return "disabled"; }
    }

    private static void closeArchive(CardStorageReader reader) throws Exception {
        // A new application process has no prior ZipFile Source in the JVM's path/mtime cache.
        var field = CardStorageReader.class.getDeclaredField("zip");
        field.setAccessible(true);
        java.util.zip.ZipFile zip = (java.util.zip.ZipFile) field.get(reader);
        if (zip != null) zip.close();
    }

    private static int parseCount(CardStorageReader reader) throws Exception {
        Method method = CardStorageReader.class.getMethod("getNameIndexParseCount");
        return (Integer) method.invoke(reader);
    }

    private static String script(String name) { return "Name:" + name + "\nManaCost:1\nTypes:Sorcery\nOracle:Fixture.\n"; }
    private static void write(Path path, String text) throws IOException { Files.writeString(path, text, StandardCharsets.UTF_8); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void archive(Path root, String name, String script) throws IOException {
        // STORED entries guarantee equal-size archives when the renamed fixtures have equal lengths.
        byte[] bytes = script.getBytes(StandardCharsets.UTF_8);
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(bytes);
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(root.resolve("cardsfolder.zip")))) {
            ZipEntry entry = new ZipEntry(name);
            entry.setMethod(ZipEntry.STORED);
            entry.setSize(bytes.length);
            entry.setCrc(crc.getValue());
            entry.setTime(0);
            out.putNextEntry(entry);
            out.write(bytes);
            out.closeEntry();
        }
    }

    public static void main(String[] args) throws Exception {
        CardStorageReaderIndexCacheTest tests = new CardStorageReaderIndexCacheTest();
        tests.warmIndexPreservesFacesAndArchivePrecedenceWithoutParsing();
        tests.sourceChangesAndCorruptionRebuildInsteadOfUsingStaleAliases();
        tests.archiveCrcInvalidatesIndexEvenWithIdenticalSizeAndTimestamp();
        tests.unavailableCacheFallsBackAndLegacyConstructorKeepsWorking();
        tests.timestampAndRootChangesInvalidateOtherwiseIdenticalInventory();
        tests.failedScriptIsRetriedAfterRecoveryWithUnchangedInventory();
        tests.wellChecksummedInvalidOrdinalsCountsAndSchemaAreRejected();
        System.out.println("CardStorageReaderIndexCacheTest: all checks passed");
    }
}
