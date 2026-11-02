package forge.adventure.editor;

import org.testng.annotations.Test;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import com.badlogic.gdx.files.FileHandle;
import static org.testng.Assert.*;

public class SwingAtlasTest {
    @Test public void previewPixelsAreScaledIntoSmallDetachedImages() throws Exception {
        var root=Files.createTempDirectory("editor-atlas-preview-");
        BufferedImage page=new BufferedImage(64,32,BufferedImage.TYPE_INT_ARGB);
        var graphics=page.createGraphics();
        graphics.setColor(Color.RED); graphics.fillRect(0,0,32,32);
        graphics.setColor(Color.BLUE); graphics.fillRect(32,0,32,32); graphics.dispose();
        ImageIO.write(page,"png",root.resolve("page.png").toFile());
        Files.writeString(root.resolve("page.atlas"),"page.png\nsize: 64,32\nformat: RGBA8888\nfilter: Nearest,Nearest\nrepeat: none\n"
                +"red\n  xy: 0,0\n  size: 32,32\nblue\n  xy: 32,0\n  size: 32,32\n");
        var atlas=new SwingAtlas(new FileHandle(root.resolve("page.atlas").toFile()),16);
        assertTrue(atlas.get("red").getImage() instanceof BufferedImage,
                "Lazy scaled icons retain a decoded atlas page per region");
        var red=(BufferedImage)atlas.get("red").getImage();
        var blue=(BufferedImage)atlas.get("blue").getImage();
        assertEquals(red.getRGB(8,8),Color.RED.getRGB());
        assertEquals(blue.getRGB(8,8),Color.BLUE.getRGB());
        assertEquals(red.getRaster().getDataBuffer().getSize(),16*16,"Preview retained the source page raster");
    }
}
