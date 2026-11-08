package forge.adventure.character;

import forge.util.MyRandom;
import org.testng.annotations.Test;
import java.util.Random;
import static org.testng.Assert.*;

public class PreviewRandomTest {
    @Test public void previewRandomIsLocalAndDoesNotAdvanceTheGameRandomCounter() {
        Random original = MyRandom.getRandom(), preview = new Random(42);
        long before = MyRandom.callCount();
        MyRandom.withRandom(preview, () -> {
            assertSame(MyRandom.getRandom(), preview);
            MyRandom.getRandom().nextInt();
            return null;
        });
        assertSame(MyRandom.getRandom(), original);
        assertEquals(MyRandom.callCount(), before);
    }
    @Test public void nestedScopesRestoreTheOuterProviderEvenAfterFailure() {
        Random original=MyRandom.getRandom(), outer=new Random(1), inner=new Random(2);
        MyRandom.withRandom(outer, () -> {
            try { MyRandom.withRandom(inner, () -> { assertSame(MyRandom.getRandom(),inner); throw new IllegalStateException("probe"); }); }
            catch (IllegalStateException expected) { assertEquals(expected.getMessage(),"probe"); }
            assertSame(MyRandom.getRandom(),outer); return null;
        });
        assertSame(MyRandom.getRandom(),original);
    }
    @Test public void changingTheProviderInsidePreviewDoesNotReplaceTheGameProvider() {
        Random original=MyRandom.getRandom(), replacement=new Random(7);
        MyRandom.withRandom(new Random(1), () -> {
            MyRandom.setRandom(replacement); assertSame(MyRandom.getRandom(),replacement); return null;
        });
        assertSame(MyRandom.getRandom(),original);
    }
}
