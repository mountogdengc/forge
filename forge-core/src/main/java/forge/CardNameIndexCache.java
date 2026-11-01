package forge;

import forge.card.CardRules;
import forge.util.BuildInfo;

import java.io.*;
import java.nio.file.*;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Disposable index metadata only: source ordinals always refer to the current inventory. */
final class CardNameIndexCache {
    // Bump when name extraction, normalization, or parser dependencies change semantics.
    private static final int VERSION = 1;
    private static final int MAGIC = 0x46434e49;
    private static final int MAX_BYTES = 64 * 1024 * 1024;
    private static final int MAX_NAMES = 1_000_000;
    private static final String FILE_NAME = "lazy-card-name-index.bin";

    static final class Index {
        final NavigableMap<String, Integer> archive;
        final NavigableMap<String, Integer> loose;

        Index(NavigableMap<String, Integer> archive, NavigableMap<String, Integer> loose) {
            this.archive = archive;
            this.loose = loose;
        }
    }

    private CardNameIndexCache() { }

    static byte[] fingerprint(File root, List<File> files, ZipFile zip) throws IOException {
        MessageDigest digest = sha256();
        try (DataOutputStream out = new DataOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(), digest))) {
            out.writeInt(VERSION);
            out.writeBoolean(BuildInfo.isDevelopmentVersion());
            // Automatic invalidation for parser builds as well as the explicit semantic version.
            for (String resource : new String[]{"/forge/card/CardRules.class", "/forge/card/CardRules$Reader.class",
                    "/forge/card/CardFace.class", "/forge/card/CardSplitType.class", "/forge/CardStorageReader.class"}) {
                try (InputStream in = CardRules.class.getResourceAsStream(resource)) {
                    if (in == null) throw new IOException("Parser version unavailable");
                    in.transferTo(out);
                }
            }
            metadata(out, root);
            out.writeInt(files.size());
            for (File file : files) metadata(out, file);
            out.writeBoolean(zip != null);
            if (zip != null) {
                metadata(out, new File(zip.getName()));
                out.writeInt(zip.size());
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    out.writeUTF(entry.getName());
                    out.writeLong(entry.getCrc());
                    out.writeLong(entry.getSize());
                    out.writeLong(entry.getCompressedSize());
                    out.writeLong(entry.getTime());
                }
            }
        }
        return digest.digest();
    }

    private static void metadata(DataOutputStream out, File file) throws IOException {
        Path path = file.toPath().toAbsolutePath().normalize();
        var attributes = java.nio.file.Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class);
        out.writeUTF(path.toString());
        out.writeLong(attributes.size());
        out.writeUTF(attributes.lastModifiedTime().toString());
    }

    static Index read(Path directory, byte[] fingerprint, int archiveSources, int looseSources) {
        try {
            byte[] bytes;
            try (InputStream in = java.nio.file.Files.newInputStream(directory.resolve(FILE_NAME))) {
                bytes = in.readNBytes(MAX_BYTES + 1);
            }
            if (bytes.length < 72 || bytes.length > MAX_BYTES) return null;
            int length = bytes.length - 32;
            MessageDigest digest = sha256();
            digest.update(bytes, 0, length);
            if (!MessageDigest.isEqual(digest.digest(), Arrays.copyOfRange(bytes, length, bytes.length))) return null;
            try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes, 0, length))) {
                if (in.readInt() != MAGIC || in.readInt() != VERSION) return null;
                if (!MessageDigest.isEqual(in.readNBytes(32), fingerprint)) return null;
                NavigableMap<String, Integer> archive = readIndex(in, archiveSources);
                NavigableMap<String, Integer> loose = readIndex(in, looseSources);
                if (in.available() != 0) return null;
                return new Index(archive, loose);
            }
        } catch (IOException | RuntimeException invalidOrUnavailable) {
            return null;
        }
    }

    private static NavigableMap<String, Integer> readIndex(DataInputStream in, int sources) throws IOException {
        int count = in.readInt();
        if (count < 0 || count > MAX_NAMES || count > in.available() / 7) throw new IOException("Invalid index count");
        NavigableMap<String, Integer> result = new TreeMap<>();
        for (int i = 0; i < count; i++) {
            String key = in.readUTF();
            int ordinal = in.readInt();
            if (key.isEmpty() || !key.matches("[a-z0-9_]+") || ordinal < 0 || ordinal >= sources
                    || result.putIfAbsent(key, ordinal) != null) throw new IOException("Invalid index entry");
        }
        return result;
    }

    static void write(Path directory, byte[] fingerprint, Index index) {
        Path temporary = null;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                out.writeInt(MAGIC);
                out.writeInt(VERSION);
                out.write(fingerprint);
                writeIndex(out, index.archive);
                writeIndex(out, index.loose);
            }
            if (bytes.size() > MAX_BYTES - 32) return;
            temporary = java.nio.file.Files.createTempFile(directory, "card-name-index-", ".tmp");
            byte[] payload = bytes.toByteArray();
            try (OutputStream out = java.nio.file.Files.newOutputStream(temporary)) {
                out.write(payload);
                out.write(sha256().digest(payload));
            }
            // Never expose a partially written cache, even on filesystems without atomic moves.
            java.nio.file.Files.move(temporary, directory.resolve(FILE_NAME), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException unavailable) {
            // A cache cannot prevent loading authoritative scripts.
        } finally {
            if (temporary != null) {
                try { java.nio.file.Files.deleteIfExists(temporary); } catch (IOException | RuntimeException ignored) { }
            }
        }
    }

    private static void writeIndex(DataOutputStream out, Map<String, Integer> index) throws IOException {
        if (index.size() > MAX_NAMES) throw new IOException("Index too large");
        out.writeInt(index.size());
        for (Map.Entry<String, Integer> entry : index.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeInt(entry.getValue());
        }
    }

    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
