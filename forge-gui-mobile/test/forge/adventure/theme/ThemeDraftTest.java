package forge.adventure.theme;

import org.testng.annotations.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.testng.Assert.*;

public class ThemeDraftTest {
    @Test public void minimalVariantContainsOnlyIntentionalOverrides() throws Exception {
        ThemeDraft draft=ThemeDraft.from(ThemeResolverTest.catalog(ThemeResolverTest.roots()),"folio");
        draft.setName("Quiet folio");draft.setOrnament("quiet");draft.setColor("text.onPaper","#302b22");
        var json=draft.toVariant("user.quiet").json();
        assertEquals(json.get("extends").getAsString(),"folio");assertFalse(json.has("colors"));
        assertEquals(json.getAsJsonObject("presentation").size(),1);
        assertEquals(json.getAsJsonObject("presentation").get("ornament").getAsString(),"quiet");
    }
    @Test public void restoringOrSwitchingPresetDropsUnsavedChanges() throws Exception {
        ThemeCatalog catalog=ThemeResolverTest.catalog(ThemeResolverTest.roots());ThemeDraft draft=ThemeDraft.from(catalog,"folio");
        draft.setName("Unsaved");draft.setTexture(0);draft.setOrnament("quiet");draft.restore();
        assertEquals(draft.resolve().name(),"Folio");assertEquals(draft.resolve().texture(),.16f);assertEquals(draft.resolve().ornament(),"full");
        assertFalse(ThemeDraft.from(catalog,"folio").toVariant("user.new").json().has("presentation"));
    }
    @Test public void invalidColorPairBlocksSaveWithRoleMessage() throws Exception {
        ThemeDraft draft=ThemeDraft.from(ThemeResolverTest.catalog(ThemeResolverTest.roots()),"folio");
        draft.setColor("action.onPrimary","#70bca5");
        assertFalse(draft.validate().isEmpty());assertTrue(draft.validate().get(0).contains("action.onPrimary"));
        assertThrows(IllegalArgumentException.class,draft::resolve);
    }
    @Test public void saveDoesNotSelectOrApply() throws Exception {
        Path root=ThemeResolverTest.roots();String[] stored={"folio"};
        AdventureThemeRuntime runtime=new AdventureThemeRuntime(root.resolve("builtins"),root.resolve("users"),stored[0],id->stored[0]=id);
        ThemeDraft draft=ThemeDraft.from(runtime.discover(),"folio");draft.setName("Quiet");draft.setOrnament("quiet");
        Path saved=ThemeStore.saveAsNew(runtime.userRoot(),runtime.discover(),draft.toVariant("user.draft"));
        assertTrue(Files.exists(saved));assertEquals(stored[0],"folio");assertEquals(runtime.resolveActive(false).ornament(),"full");
    }
    @Test public void applyUpdatesPendingButNotActiveTheme() throws Exception {
        Path root=ThemeResolverTest.roots();AdventureThemeRuntime runtime=new AdventureThemeRuntime(root.resolve("builtins"),root.resolve("users"),"folio",id->{});
        ThemeDraft draft=ThemeDraft.from(runtime.discover(),"folio");draft.setOrnament("quiet");
        Path saved=ThemeStore.saveAsNew(runtime.userRoot(),runtime.discover(),draft.toVariant("user.draft"));
        String id=ThemeManifest.parse(saved,Files.readString(saved)).id();runtime.requestSelection(id);
        assertEquals(runtime.pendingId(),id);assertEquals(runtime.resolveActive(false).id(),"folio");
    }
}
