package forge.adventure.character;

import org.testng.annotations.Test;
import java.nio.ByteBuffer;

import static org.testng.Assert.*;

public class PortraitImagesTest {
    private static byte[] png(int width, int height) {
        ByteBuffer data = ByteBuffer.allocate(32);
        data.putLong(0x89504e470d0a1a0aL).putInt(13).putInt(0x49484452).putInt(width).putInt(height);
        return data.array();
    }
    @Test public void pngDimensionsAreCheckedBeforeDecoding() {
        var size = PortraitImages.inspect(png(1200, 1800));
        assertEquals(size.width(), 1200); assertEquals(size.height(), 1800);
    }
    @Test public void jpegDimensionsAreReadAcrossMetadataSegments() {
        byte[] jpeg = new byte[]{(byte)255,(byte)216,(byte)255,(byte)224,0,4,0,0,
                (byte)255,(byte)192,0,11,8,3,32,2,88,1,1,0,0};
        var size = PortraitImages.inspect(jpeg);
        assertEquals(size.width(), 600); assertEquals(size.height(), 800);
    }
    @Test(expectedExceptions=IllegalArgumentException.class) public void oversizedDimensionsAreRejected() { PortraitImages.inspect(png(50000,50000)); }
    @Test(expectedExceptions=IllegalArgumentException.class) public void zeroDimensionsAreRejected() { PortraitImages.inspect(png(0,300)); }
    @Test(expectedExceptions=IllegalArgumentException.class) public void oversizeFileIsRejected() { PortraitImages.inspect(new byte[PortraitImages.MAX_BYTES+1]); }
    @Test(expectedExceptions=IllegalArgumentException.class) public void truncatedJpegDoesNotLoopOrReadOutsideTheFile() { PortraitImages.inspect(new byte[]{(byte)255,(byte)216,(byte)255,(byte)224,127,127}); }
    @Test(expectedExceptions=IllegalArgumentException.class) public void nonImageIsRejected() { PortraitImages.inspect("not an image".getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
}
