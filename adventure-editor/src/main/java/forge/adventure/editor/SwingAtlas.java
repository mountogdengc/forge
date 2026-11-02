package forge.adventure.editor;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;



/**
 * Editor class to edit configuration, maybe moved or removed
 */
public class SwingAtlas {

    int imageSize=32;
    HashMap<String, ArrayList<ImageIcon>> images=new  HashMap<>();
    public HashMap<String, ArrayList<ImageIcon>> getImages()
    {
        return images;
    }
    public  SwingAtlas(FileHandle path,int imageSize)
    {
        this.imageSize=imageSize;
        if(!path.exists()||!path.toString().endsWith(".atlas"))
            return;
        TextureAtlas.TextureAtlasData data=new TextureAtlas.TextureAtlasData(path,path.parent(),false);
        HashMap<String,BufferedImage> pages = new HashMap<>();
        for(TextureAtlas.TextureAtlasData.Region region: new Array.ArrayIterator<>(data.getRegions()))
        {
             String name=region.name;
             if(!images.containsKey(name))
             {
                 images.put(name,new ArrayList<>());
             }
            ArrayList<ImageIcon> imageList=images.get(name);
            try
            {
                String pagePath = region.page.textureFile.path();
                if (!pages.containsKey(pagePath)) pages.put(pagePath, ImageIO.read(region.page.textureFile.file()));
                BufferedImage page = pages.get(pagePath);
                if (page != null) imageList.add(spriteToImage(region, page));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    public  SwingAtlas(FileHandle path)
    {
        this(path,32);
    }

    private ImageIcon spriteToImage(TextureAtlas.TextureAtlasData.Region sprite, BufferedImage page) {
        try
        {
            int longest = Math.max(sprite.width,sprite.height);
            int width = Math.max(1,Math.round(imageSize * sprite.width/(float)longest));
            int height = Math.max(1,Math.round(imageSize * sprite.height/(float)longest));
            BufferedImage icon = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = icon.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                graphics.drawImage(page,0,0,width,height,sprite.left,sprite.top,sprite.left+sprite.width,sprite.top+sprite.height,null);
            } finally { graphics.dispose(); }
            return new ImageIcon(icon);

        }
        catch (Exception e)
        {
            return null;
        }
}

    public ImageIcon get(String name) {
        if(images.get(name).size()==0)
            return null;
        return images.get(name).get(0);
    }

    public boolean has(String name) {
        return images.containsKey(name);
    }

    public ImageIcon getAny() {
        if(images.isEmpty())
            return null;
        ArrayList<ImageIcon> imageList= images.get(images.keySet().iterator().next());
        if(imageList.isEmpty())
            return null;
        return imageList.get(0);
    }
}
