package forge.adventure.theme;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Disposable;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ThemeOwnershipTest {
    @Test public void disposingAdapterDoesNotDisposeBorrowedFontOrTexture() throws Exception {
        Skin legacy=ThemeSkinAdapterTest.legacy();
        // Real Skin.dispose traverses Disposable resources. An adapter must not do that to its parent.
        boolean[] disposed={false};Disposable asset=()->disposed[0]=true;
        legacy.add("borrowed",asset,Disposable.class);
        Skin adapter=ThemeSkinAdapter.adapt(legacy,ThemeSkinAdapterTest.theme());
        adapter.dispose();adapter.dispose();
        assertFalse(disposed[0]);
        assertEquals(legacy.getFont("default").getData().scaleX,1f);
        assertSame(legacy.get("borrowed",Disposable.class),asset);
    }
}
