package forge.adventure.theme;

import org.testng.annotations.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.testng.Assert.*;

public class ThemeResolverTest {
    static String bitmapFont(String page){return "info face=Fixture size=12 padding=0,0,0,0 spacing=0,0\ncommon lineHeight=12 base=10 scaleW=1 scaleH=1 pages=1 packed=0\n"+page+"\nchars count=2\nchar id=32 x=0 y=0 width=0 height=0 xoffset=0 yoffset=0 xadvance=4 page=0 chnl=0\nchar id=65 x=0 y=0 width=1 height=1 xoffset=0 yoffset=0 xadvance=4 page=0 chnl=0\n";}
    @Test public void indentedBitmapFontPagesCannotEscapeTheOwningTheme() throws Exception {
        Path root=roots();Files.write(root.resolve("outside.png"),new byte[]{1});
        Path owner=manifest(root.resolve("users"),"indented",child("user.indented","folio",",\"fonts\":{\"body\":\"font.fnt\"}")).getParent();
        Files.writeString(owner.resolve("font.fnt"),bitmapFont(" page id=0 file=\"../../outside.png\""));
        assertEquals(new com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData(
                new com.badlogic.gdx.files.FileHandle(owner.resolve("font.fnt").toFile()),false).getImagePaths().length,1,
                "The escape fixture must be a valid descriptor for the native loader");
        assertThrows(IllegalArgumentException.class,()->ThemeResolver.resolve(catalog(root),"user.indented"));
    }
    // Mutations caught: losing child overrides/provenance, accepting unsafe paths,
    // using edited live files instead of a startup snapshot, and skipping contrast.
    static final String COLORS = """
        "surface.canvas":"#102820", "surface.panel":"#203630", "surface.paper":"#ddd0ad",
        "text.primary":"#f1e6ce", "text.secondary":"#c4d0bd", "text.onPaper":"#302b22",
        "border.frame":"#ac8953", "border.frameText":"#101810", "border.highlight":"#e3c892",
        "action.primary":"#70bca5", "action.onPrimary":"#102820",
        "action.secondary":"#203630", "action.onSecondary":"#f1e6ce",
        "state.hover":"#305046", "state.selected":"#70bca5", "state.onSelected":"#102820",
        "state.disabled":"#35433d", "state.onDisabled":"#d5d9cf", "state.focus":"#e3c892",
        "phase.activeEnabled":"#70bca5", "phase.activeDisabled":"#ddd0ad",
        "phase.inactiveEnabled":"#354b41", "phase.inactiveDisabled":"#203630",
        "target.normal":"#80c9ea", "target.combat":"#ef9b91", "target.planeswalker":"#e2c06a"
        """;

    static Path roots() throws Exception {
        Path root=Files.createTempDirectory("theme-resolver-");
        manifest(root.resolve("builtins"),"folio", "{\"schemaVersion\":1,\"id\":\"folio\",\"name\":\"Folio\",\"colors\":{"
                +COLORS+"},\"fonts\":{\"body\":\"legacy:default\",\"heading\":\"legacy:default\"},"
                +"\"components\":{\"shape\":\"round\"},\"presentation\":{\"ornament\":\"full\",\"paperTexture\":0.16}}");
        return root;
    }
    static Path manifest(Path root,String folder,String json) throws Exception {
        Path path=Files.createDirectories(root.resolve(folder)).resolve("theme.json");
        Files.writeString(path,json);
        return path;
    }
    static String child(String id,String parent,String extra) {
        return "{\"schemaVersion\":1,\"id\":\""+id+"\",\"name\":\""+id+"\",\"extends\":\""+parent+"\""+extra+"}";
    }
    static ThemeCatalog catalog(Path root) { return ThemeCatalog.snapshot(root.resolve("builtins"),root.resolve("users")); }

    @Test public void childOverridesOneRoleAndInheritsTheRest() throws Exception {
        Path root=roots();
        manifest(root.resolve("users"),"moon",child("user.moon","folio",",\"colors\":{\"text.onPaper\":\"#201b12\"}"));
        ResolvedTheme theme=ThemeResolver.resolve(catalog(root),"user.moon");
        assertEquals(theme.colors().get("text.onPaper"),"#201b12");
        assertEquals(theme.colors().get("surface.panel"),"#203630");
        assertEquals(theme.components().get("shape"),"round");
        assertThrows(UnsupportedOperationException.class,()->theme.colors().put("surface.panel","#ffffff"));
    }
    @Test public void inheritedAssetKeepsParentDirectory() throws Exception {
        Path root=roots();
        Path parent=manifest(root.resolve("users"),"parent",child("user.parent","folio",",\"materials\":{\"panel\":\"tile.png\"}"));
        Files.write(parent.getParent().resolve("tile.png"),new byte[]{1});
        manifest(root.resolve("users"),"child",child("user.child","user.parent",""));
        assertEquals(ThemeResolver.resolve(catalog(root),"user.child").assets().get("panel"),parent.getParent().resolve("tile.png").toRealPath());
    }
    @Test public void eightLevelsAllowedNinthRejected() throws Exception {
        Path root=roots();String parent="folio";
        for(int i=1;i<=8;i++){String id="user.level"+i;manifest(root.resolve("users"),id,child(id,parent,""));parent=id;}
        ThemeCatalog cat=catalog(root);
        assertEquals(ThemeResolver.resolve(cat,"user.level7").id(),"user.level7");
        assertThrows(IllegalArgumentException.class,()->ThemeResolver.resolve(cat,"user.level8"));
    }
    @Test public void cycleMissingParentAndDuplicateIdsRejected() throws Exception {
        Path root=roots();
        manifest(root.resolve("users"),"a",child("user.a","user.b",""));
        manifest(root.resolve("users"),"b",child("user.b","user.a",""));
        manifest(root.resolve("users"),"missing",child("user.missing","nowhere",""));
        manifest(root.resolve("users"),"d1",child("user.duplicate","folio",""));
        manifest(root.resolve("users"),"d2",child("user.duplicate","folio",""));
        ThemeCatalog cat=catalog(root);
        for(String id:new String[]{"user.a","user.missing","user.duplicate"})
            assertThrows(IllegalArgumentException.class,()->ThemeResolver.resolve(cat,id));
        assertEquals(ThemeResolver.resolve(cat,"folio").id(),"folio","Invalid users must not disable a valid built-in");
    }
    @Test public void invalidVersionAndUnknownGameplaySectionsRejected() throws Exception {
        Path root=roots();
        manifest(root.resolve("users"),"future",child("user.future","folio","").replace("\"schemaVersion\":1","\"schemaVersion\":2"));
        manifest(root.resolve("users"),"rules",child("user.rules","folio",",\"goldMultiplier\":4"));
        manifest(root.resolve("users"),"unquoted","{schemaVersion:1,id:\"user.unquoted\",name:\"Invalid\",extends:\"folio\"}");
        for(String id:new String[]{"user.future","user.rules","user.unquoted"})
            assertThrows(IllegalArgumentException.class,()->ThemeResolver.resolve(catalog(root),id));
    }
    @Test public void assetTraversalAbsoluteAndUrlRejected() throws Exception {
        Path root=roots();Files.write(root.resolve("outside.png"),new byte[]{1});
        String[] unsafe={"../../outside.png","https://example.com/tile.png",root.resolve("outside.png").toString().replace('\\','/')};
        for(int i=0;i<unsafe.length;i++){
            String id="user.unsafe"+i;
            manifest(root.resolve("users"),id,child(id,"folio",",\"materials\":{\"panel\":\""+unsafe[i]+"\"}"));
            assertThrows(IllegalArgumentException.class,()->ThemeResolver.resolve(catalog(root),id));
        }
    }
    @Test public void symbolicLinkEscapeRejected() throws Exception {
        Path root=roots();Files.write(root.resolve("outside.png"),new byte[]{1});
        Path owner=manifest(root.resolve("users"),"link",child("user.link","folio",",\"materials\":{\"panel\":\"tile.png\"}")).getParent();
        try { Files.createSymbolicLink(owner.resolve("tile.png"),root.resolve("outside.png")); }
        catch(java.nio.file.FileSystemException|UnsupportedOperationException unavailable){throw new org.testng.SkipException("Host cannot create symbolic links",unavailable);}
        assertThrows(IllegalArgumentException.class,()->ThemeResolver.resolve(catalog(root),"user.link"));
    }
    @Test public void fontPagesAreCheckedForQuotedUnquotedAndUppercaseFiles() throws Exception {
        Path root=roots();
        Path owner=manifest(root.resolve("users"),"font",child("user.font","folio",",\"fonts\":{\"body\":\"font.FNT\"}")).getParent();
        for(String page:new String[]{"page id=0 file=../../outside.png", "page id=0 file=\"../../outside.png\""}){
            Files.writeString(owner.resolve("font.FNT"),page);
            assertThrows(IllegalArgumentException.class,()->ThemeResolver.resolve(catalog(root),"user.font"));
        }
        Files.write(owner.resolve("page.png"),new byte[]{1});
        Files.writeString(owner.resolve("font.FNT"),bitmapFont("page id=0 file=\"page.png\""));
        assertEquals(ThemeResolver.resolve(catalog(root),"user.font").id(),"user.font");
    }
    @Test public void atlasAbsolutePageCannotBypassDependencyValidation() throws Exception {
        Path root=roots();Path outside=Files.write(root.resolve("outside.png"),new byte[]{1});
        Path owner=manifest(root.resolve("users"),"absolute-atlas",child("user.absolute-atlas","folio",",\"materials\":{\"panel\":\"skin.atlas\"}")).getParent();
        Files.writeString(owner.resolve("skin.atlas"),outside.toString().replace('\\','/')+"\nsize: 1,1\nformat: RGBA8888\nfilter: Nearest,Nearest\nrepeat: none\n");
        assertThrows(IllegalArgumentException.class,()->ThemeResolver.resolve(catalog(root),"user.absolute-atlas"));
    }
    @Test public void atlasCannotReferenceAnotherAtlasAsAnImagePage() throws Exception {
        Path root=roots();
        Path owner=manifest(root.resolve("users"),"atlas",child("user.atlas","folio",",\"materials\":{\"panel\":\"a.atlas\"}")).getParent();
        Files.writeString(owner.resolve("a.atlas"),"b.atlas\nsize: 1,1\n");
        Files.writeString(owner.resolve("b.atlas"),"image.png\nsize: 1,1\n");
        Files.write(owner.resolve("image.png"),new byte[]{1});
        assertThrows(IllegalArgumentException.class,()->ThemeResolver.resolve(catalog(root),"user.atlas"));
    }
    @Test public void windowsJunctionCannotEscapeTheOwningTheme() throws Exception {
        if(!System.getProperty("os.name").toLowerCase().contains("windows"))throw new org.testng.SkipException("Windows junction check");
        Path root=roots(),outside=Files.createDirectories(root.resolve("outside"));Files.write(outside.resolve("page.png"),new byte[]{1});
        Path owner=manifest(root.resolve("users"),"junction",child("user.junction","folio",",\"materials\":{\"panel\":\"assets/page.png\"}")).getParent();
        Process process=new ProcessBuilder("cmd.exe","/d","/c","mklink","/J",owner.resolve("assets").toString(),outside.toString()).redirectErrorStream(true).start();
        String diagnostic=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
        if(process.waitFor()!=0)throw new org.testng.SkipException("Cannot create test junction: "+diagnostic);
        assertThrows(IllegalArgumentException.class,()->ThemeResolver.resolve(catalog(root),"user.junction"));
    }
    @Test public void immutableCatalogIgnoresLaterManifestEdits() throws Exception {
        Path root=roots();ThemeCatalog snapshot=catalog(root);
        Files.writeString(root.resolve("builtins/folio/theme.json"),"{}");
        assertEquals(ThemeResolver.resolve(snapshot,"folio").colors().get("surface.paper"),"#ddd0ad");
    }
    @Test public void contrastChecksAllDeclaredForegroundPairs() throws Exception {
        Path root=roots();
        String[] roles={"text.primary","text.onPaper","action.onPrimary","action.onSecondary","state.onSelected","state.onDisabled","border.frameText"};
        String[] backgrounds={"#203630","#ddd0ad","#70bca5","#203630","#70bca5","#35433d","#ac8953"};
        for(int i=0;i<roles.length;i++){
            String id="user.lowcontrast"+i;
            manifest(root.resolve("users"),id,child(id,"folio",",\"colors\":{\""+roles[i]+"\":\""+backgrounds[i]+"\"}"));
            assertThrows(IllegalArgumentException.class,()->ThemeResolver.resolve(catalog(root),id));
        }
    }
    @Test public void rejectedThemeCannotExposePartialValues() throws Exception {
        Path root=roots();
        manifest(root.resolve("users"),"bad",child("user.bad","folio",",\"colors\":{\"text.primary\":\"potato\"}"));
        assertThrows(IllegalArgumentException.class,()->ThemeResolver.resolve(catalog(root),"user.bad"));
        assertEquals(ThemeResolver.resolve(catalog(root),"folio").colors().get("text.primary"),"#f1e6ce");
    }
    @Test public void hoverForegroundAndAssetRoleTypesMustBeValid() throws Exception {
        Path root=roots();
        manifest(root.resolve("users"),"hover",child("user.hover","folio",",\"colors\":{\"state.hover\":\"#f1e6ce\"}"));
        assertThrows(IllegalArgumentException.class,()->ThemeResolver.resolve(catalog(root),"user.hover"));
        Path owner=manifest(root.resolve("users"),"wrong",child("user.wrong","folio",",\"fonts\":{\"body\":\"image.png\"}")).getParent();
        Files.write(owner.resolve("image.png"),new byte[]{1});
        assertThrows(IllegalArgumentException.class,()->ThemeResolver.resolve(catalog(root),"user.wrong"));
        Files.writeString(owner.resolve("font.fnt"),"info face=Example");
        manifest(root.resolve("users"),"wrong",child("user.wrong","folio",",\"materials\":{\"panel\":\"font.fnt\"}"));
        assertThrows(IllegalArgumentException.class,()->ThemeResolver.resolve(catalog(root),"user.wrong"));
    }
}
