package forge.adventure.theme;

import org.testng.annotations.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.testng.Assert.*;

public class AdventureThemeRuntimeTest {
    @Test public void rejectedNativeResourcesFallBackOnceWithoutChangingPendingChoice() throws Exception {
        Path root=ThemeResolverTest.roots();
        ThemeResolverTest.manifest(root.resolve("users"),"broken",ThemeResolverTest.child("user.broken","folio",""));
        AdventureThemeRuntime runtime=new AdventureThemeRuntime(root.resolve("builtins"),root.resolve("users"),"user.broken",id->{});
        assertEquals(runtime.resolveActive(false).id(),"user.broken");
        runtime.rejectTheme("user.broken","Image cannot be decoded");
        assertEquals(runtime.resolveActive(false).id(),"folio");assertEquals(runtime.pendingId(),"user.broken");
        runtime.rejectTheme("folio","Font cannot be decoded");
        assertNull(runtime.resolveActive(false));assertEquals(runtime.activeId(),"campaign");
        assertTrue(runtime.diagnostics().stream().anyMatch(s->s.contains("Image cannot be decoded")));
    }
    @Test public void startupChoiceSurvivesMenuAndModeTransitions() throws Exception {
        Path root=ThemeResolverTest.roots();
        ThemeResolverTest.manifest(root.resolve("builtins"),"midnight",ThemeResolverTest.child("midnight","folio",",\"components\":{\"shape\":\"angular\"}"));
        String[] stored={"folio"};
        AdventureThemeRuntime runtime=new AdventureThemeRuntime(root.resolve("builtins"),root.resolve("users"),stored[0],id->stored[0]=id);
        runtime.requestSelection("midnight");
        assertEquals(stored[0],"midnight");assertEquals(runtime.pendingId(),"midnight");
        for(boolean bespoke:new boolean[]{false,true,false})assertEquals(runtime.resolveActive(bespoke).shape(),"round");
        AdventureThemeRuntime restarted=new AdventureThemeRuntime(root.resolve("builtins"),root.resolve("users"),stored[0],id->stored[0]=id);
        assertEquals(restarted.resolveActive(false).shape(),"angular");
    }
    @Test public void savingASelectedRecipeCannotChangeItsActiveBytes() throws Exception {
        Path root=ThemeResolverTest.roots();
        Path recipe=ThemeResolverTest.manifest(root.resolve("users"),"saved",ThemeResolverTest.child("user.saved","folio",",\"presentation\":{\"ornament\":\"quiet\"}"));
        AdventureThemeRuntime runtime=new AdventureThemeRuntime(root.resolve("builtins"),root.resolve("users"),"user.saved",id->{});
        Files.writeString(recipe,ThemeResolverTest.child("user.saved","folio",",\"presentation\":{\"ornament\":\"full\"}"));
        assertEquals(runtime.resolveActive(false).ornament(),"quiet");
    }
    @Test public void newThemeSelectableButPendingUntilNewRuntime() throws Exception {
        Path root=ThemeResolverTest.roots();
        AdventureThemeRuntime runtime=new AdventureThemeRuntime(root.resolve("builtins"),root.resolve("users"),"folio",id->{});
        ThemeResolverTest.manifest(root.resolve("users"),"new",ThemeResolverTest.child("user.new","folio",",\"components\":{\"shape\":\"leaf\"}"));
        runtime.requestSelection("user.new");
        assertEquals(runtime.pendingId(),"user.new");assertEquals(runtime.resolveActive(false).id(),"folio");
        assertThrows(IllegalArgumentException.class,()->runtime.requestSelection("does.not.exist"));
        assertEquals(runtime.pendingId(),"user.new");
    }
    @Test public void missingStartupSelectionFallsBackWithNotice() throws Exception {
        Path root=ThemeResolverTest.roots();
        AdventureThemeRuntime runtime=new AdventureThemeRuntime(root.resolve("builtins"),root.resolve("users"),"gone",id->{});
        assertEquals(runtime.resolveActive(false).id(),"folio");assertFalse(runtime.diagnostics().isEmpty());
    }
    @Test public void automaticCompatibilityDoesNotBecomeExplicitPreference() throws Exception {
        Path root=ThemeResolverTest.roots();String[] stored={null};
        AdventureThemeRuntime runtime=new AdventureThemeRuntime(root.resolve("builtins"),root.resolve("users"),null,id->stored[0]=id);
        assertNull(runtime.resolveActive(true));assertEquals(runtime.activeId(),"campaign");
        assertEquals(runtime.resolveActive(false).id(),"folio");assertNull(stored[0]);
        runtime.requestSelection("folio");assertEquals(stored[0],"folio");
    }
}
