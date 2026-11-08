package forge.adventure.character;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import forge.adventure.data.CharacterCreationData.Portrait;
import forge.adventure.data.HeroListData;
import forge.adventure.util.Config;
import forge.adventure.util.Serializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Header checks run before allocating an image or a GPU texture. */
public final class PortraitImages {
    public static final int MAX_BYTES = 10 * 1024 * 1024;
    public static final int MAX_DIMENSION = 4096;
    public record Dimensions(int width, int height) { }

    private PortraitImages() { }

    public static Dimensions inspect(byte[] bytes) {
        if (bytes == null || bytes.length < 8 || bytes.length > MAX_BYTES)
            throw new IllegalArgumentException("Choose a PNG or JPEG no larger than 10 MiB.");
        if (bytes.length >= 24 && ByteBuffer.wrap(bytes).getLong() == 0x89504e470d0a1a0aL
                && ByteBuffer.wrap(bytes, 12, 4).getInt() == 0x49484452) {
            return dimensions(ByteBuffer.wrap(bytes, 16, 4).getInt(), ByteBuffer.wrap(bytes, 20, 4).getInt());
        }
        if ((bytes[0] & 255) == 255 && (bytes[1] & 255) == 216) {
            int offset = 2;
            while (offset + 3 < bytes.length) {
                if ((bytes[offset++] & 255) != 255) break;
                while (offset < bytes.length && (bytes[offset] & 255) == 255) offset++;
                if (offset >= bytes.length) break;
                int marker = bytes[offset++] & 255;
                if (marker == 217 || marker == 218) break;
                if (marker == 1 || marker >= 208 && marker <= 215) continue;
                if (offset + 2 > bytes.length) break;
                int length = unsignedShort(bytes, offset);
                if (length < 2 || offset + length > bytes.length) break;
                if (marker >= 192 && marker <= 207 && marker != 196 && marker != 200 && marker != 204) {
                    if (length < 7) break;
                    return dimensions(unsignedShort(bytes, offset + 5), unsignedShort(bytes, offset + 3));
                }
                offset += length;
            }
        }
        throw new IllegalArgumentException("This file is not a supported PNG or JPEG image.");
    }

    private static int unsignedShort(byte[] bytes, int offset) {
        return (bytes[offset] & 255) * 256 + (bytes[offset + 1] & 255);
    }

    private static Dimensions dimensions(int width, int height) {
        if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION)
            throw new IllegalArgumentException("Portrait dimensions must be between 1 and 4096 pixels.");
        return new Dimensions(width, height);
    }

    /** Called on the render thread; the detached result belongs to the eventual playthrough, not the source file. */
    public static byte[] importImage(Path file) throws IOException {
        byte[] bytes;
        try (var input = Files.newInputStream(file)) { bytes = input.readNBytes(MAX_BYTES + 1); }
        inspect(bytes);
        Pixmap source = new Pixmap(bytes, 0, bytes.length);
        Pixmap square = new Pixmap(512, 512, Pixmap.Format.RGBA8888);
        try {
            int size = Math.min(source.getWidth(), source.getHeight());
            square.setFilter(Pixmap.Filter.BiLinear);
            square.setBlending(Pixmap.Blending.None);
            square.drawPixmap(source, (source.getWidth() - size) / 2, (source.getHeight() - size) / 2,
                    size, size, 0, 0, 512, 512);
            return Serializer.EncodePixmap(square, false);
        } finally { source.dispose(); square.dispose(); }
    }

    public static final class Handle implements Disposable {
        public final TextureRegion region;
        private final Texture owned;
        private Handle(TextureRegion region, Texture owned) { this.region = region; this.owned = owned; }
        @Override public void dispose() { if (owned != null) owned.dispose(); }
    }

    public static Handle load(Config config, HeroListData heroes, Portrait portrait, byte[] imported) {
        if (imported != null && imported.length > 0) {
            inspect(imported);
            Pixmap pixels = new Pixmap(imported, 0, imported.length);
            try {
                Texture texture = new Texture(pixels);
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                return new Handle(new TextureRegion(texture), texture);
            } finally { pixels.dispose(); }
        }
        if (portrait == null) throw new IllegalArgumentException("Choose a portrait.");
        if (portrait.legacy) return new Handle(heroes.getAvatar(portrait.heroIndex, portrait.female, portrait.avatarIndex), null);
        Texture texture = new Texture(resource(config, portrait.image));
        try {
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            int width = portrait.width == 0 ? texture.getWidth() : portrait.width;
            int height = portrait.height == 0 ? texture.getHeight() : portrait.height;
            if (portrait.x < 0 || portrait.y < 0 || width <= 0 || height <= 0
                    || (long) portrait.x + width > texture.getWidth() || (long) portrait.y + height > texture.getHeight())
                throw new IllegalArgumentException("Portrait region is outside its image: " + portrait.id);
            return new Handle(new TextureRegion(texture, portrait.x, portrait.y, width, height), texture);
        } catch (RuntimeException error) { texture.dispose(); throw error; }
    }

    public static FileHandle resource(Config config, String reference) {
        if (reference == null || reference.isBlank() || Path.of(reference).isAbsolute())
            throw new IllegalArgumentException("A campaign-relative image or sprite path is required.");
        FileHandle handle = config.getFile(reference);
        try {
            Path candidate = handle.file().toPath().toRealPath();
            Path campaign = Path.of(config.getFilePath("")).toRealPath();
            Path common = Path.of(config.getCommonFilePath("")).toRealPath();
            if (!candidate.startsWith(campaign) && !candidate.startsWith(common))
                throw new IllegalArgumentException("Character resource is outside the campaign/common folders.");
            return handle;
        } catch (IOException error) { throw new IllegalArgumentException("Character resource is missing: " + reference, error); }
    }
}
