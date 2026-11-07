package forge.adventure.theme;

import org.testng.annotations.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.testng.Assert.*;

public class ThemeStoreTest {
    @Test public void saveAsNewWritesOnlyThemeFolder() throws Exception {
        Path root=ThemeResolverTest.roots();
        String original=Files.readString(root.resolve("builtins/folio/theme.json"));
        ThemeManifest variant=ThemeManifest.parse(root.resolve("draft.json"),ThemeResolverTest.child("user.draft","folio",",\"presentation\":{\"ornament\":\"quiet\"}"));
        Path saved=ThemeStore.saveAsNew(root.resolve("users"),ThemeResolverTest.catalog(root),variant);
        assertTrue(saved.startsWith(root.resolve("users")));
        String id=ThemeManifest.parse(saved,Files.readString(saved)).id();
        assertTrue(id.startsWith("user."));assertNotEquals(id,"user.draft");
        assertEquals(ThemeResolver.resolve(ThemeResolverTest.catalog(root),id).ornament(),"quiet");
        assertEquals(Files.readString(root.resolve("builtins/folio/theme.json")),original);
    }
    @Test public void duplicateDisplayNameCannotOverwriteTheme() throws Exception {
        Path root=ThemeResolverTest.roots();ThemeCatalog cat=ThemeResolverTest.catalog(root);
        ThemeManifest draft=ThemeManifest.parse(root.resolve("draft.json"),ThemeResolverTest.child("user.draft","folio",""));
        Path first=ThemeStore.saveAsNew(root.resolve("users"),cat,draft);
        String original=Files.readString(first);
        Path second=ThemeStore.saveAsNew(root.resolve("users"),ThemeResolverTest.catalog(root),draft);
        assertNotEquals(second,first);assertEquals(Files.readString(first),original);
        assertEquals(ThemeManifest.parse(second,Files.readString(second)).name(),"user.draft");
    }
    @Test public void failedValidationLeavesNoPublishedTheme() throws Exception {
        Path root=ThemeResolverTest.roots();
        ThemeManifest bad=ThemeManifest.parse(root.resolve("draft.json"),ThemeResolverTest.child("user.bad","folio",",\"colors\":{\"text.primary\":\"#203630\"}"));
        assertThrows(IllegalArgumentException.class,()->ThemeStore.saveAsNew(root.resolve("users"),ThemeResolverTest.catalog(root),bad));
        assertFalse(Files.exists(root.resolve("users")),"Validate before creating even the user theme root");
    }
    @Test public void selectionPreservesUnrelatedSettingsAndSaveBytes() throws Exception {
        Path root=Files.createTempDirectory("theme-settings-");Path settings=root.resolve("settings.json");
        Files.writeString(settings,"{width:1600,fullScreen:true,dayNightBG:true,futureSetting:{keep:42},adventureThemeId:folio}");
        Path save=root.resolve("save.sav");Files.writeString(save,"unchanged-save");
        ThemeStore.writeSelection(settings,"midnight");
        var json=com.google.gson.JsonParser.parseString(Files.readString(settings)).getAsJsonObject();
        assertEquals(json.get("width").getAsInt(),1600);assertTrue(json.get("fullScreen").getAsBoolean());
        assertTrue(json.get("dayNightBG").getAsBoolean());assertEquals(json.getAsJsonObject("futureSetting").get("keep").getAsInt(),42);
        assertEquals(ThemeStore.readSelection(settings),"midnight");assertEquals(Files.readString(save),"unchanged-save");
    }
}
