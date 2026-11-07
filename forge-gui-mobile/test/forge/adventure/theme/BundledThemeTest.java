package forge.adventure.theme;

import org.testng.annotations.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.testng.Assert.*;

public class BundledThemeTest {
    static Path builtins(){Path p=Path.of("../forge-gui/res/adventure/common/themes");return Files.isDirectory(p)?p:Path.of("forge-gui/res/adventure/common/themes");}
    @Test public void allBundledThemesResolveAndPassContrast(){
        ThemeCatalog catalog=ThemeCatalog.snapshot(builtins(),null);
        for(String id:new String[]{"folio","midnight","verdant"}){
            ResolvedTheme theme=ThemeResolver.resolve(catalog,id);
            assertTrue(ThemeResolver.contrast(theme.color("text.primary"),theme.color("surface.panel"))>=4.5);
            assertTrue(ThemeResolver.contrast(theme.color("text.onPaper"),theme.color("surface.paper"))>=4.5);
        }
        assertTrue(catalog.diagnostics().isEmpty(),catalog.diagnostics().toString());
    }
    @Test public void variantsHaveDifferentShapesAndMaterials(){
        ThemeCatalog cat=ThemeCatalog.snapshot(builtins(),null);
        assertEquals(ThemeResolver.resolve(cat,"folio").shape(),"round");
        assertEquals(ThemeResolver.resolve(cat,"midnight").shape(),"angular");
        assertEquals(ThemeResolver.resolve(cat,"verdant").shape(),"leaf");
        assertNotEquals(ThemeResolver.resolve(cat,"midnight").color("surface.paper"),ThemeResolver.resolve(cat,"folio").color("surface.paper"));
        assertNotEquals(ThemeResolver.resolve(cat,"verdant").texture(),ThemeResolver.resolve(cat,"folio").texture());
    }
}
