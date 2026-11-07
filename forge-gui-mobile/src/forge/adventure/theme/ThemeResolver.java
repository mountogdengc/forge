package forge.adventure.theme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

/** Resolves recipes without starting a renderer, game, or card catalog. */
public final class ThemeResolver {
    private ThemeResolver() { }
    public static ResolvedTheme resolve(ThemeCatalog catalog, String id) {
        return resolve(catalog, id, new LinkedHashSet<>());
    }
    private static ResolvedTheme resolve(ThemeCatalog catalog, String id, Set<String> chain) {
        ThemeCatalog.Entry entry = catalog.entry(id);
        if (entry == null) throw new IllegalArgumentException("Theme '" + id + "' is missing, invalid, or duplicated. " + catalog.diagnostics());
        if (chain.size() >= 8 || !chain.add(id)) throw invalid(entry, "extends", "Inheritance cycle or chain exceeds eight manifests: " + chain);
        ThemeManifest manifest = entry.manifest();
        Map<String,String> colors = new LinkedHashMap<>(), fonts = new LinkedHashMap<>(), components = new LinkedHashMap<>(), presentation = new LinkedHashMap<>();
        Map<String,Path> assets = new LinkedHashMap<>();
        if (manifest.parent() != null) {
            ResolvedTheme parent = resolve(catalog, manifest.parent(), chain);
            colors.putAll(parent.colors()); fonts.putAll(parent.fonts()); assets.putAll(parent.assets());
            components.putAll(parent.components()); presentation.putAll(parent.presentation());
        }
        chain.remove(id);
        colors.putAll(manifest.section("colors"));
        components.putAll(manifest.section("components")); presentation.putAll(manifest.section("presentation"));
        for (var value : manifest.section("materials").entrySet()) {
            if (!ThemeManifest.ASSET_SLOTS.contains(value.getKey())) throw invalid(entry,"materials."+value.getKey(),"Unknown presentation slot");
            if(!value.getValue().toLowerCase(Locale.ROOT).matches(".*\\.(png|jpg|jpeg|atlas)$"))throw invalid(entry,"materials."+value.getKey(),"Expected image or atlas artwork");
            assets.put(value.getKey(), asset(entry,value.getValue()));
        }
        for (var font : manifest.section("fonts").entrySet()) {
            if (!Set.of("body","heading").contains(font.getKey())) throw invalid(entry,"fonts."+font.getKey(),"Unknown font role");
            if (font.getValue().equals("legacy:default")) fonts.put(font.getKey(), font.getValue());
            else {
                if(!font.getValue().toLowerCase(Locale.ROOT).matches(".*\\.(fnt|ttf|otf)$"))throw invalid(entry,"fonts."+font.getKey(),"Expected a bitmap or outline font");
                fonts.put(font.getKey(), asset(entry,font.getValue()).toString());
            }
        }
        validateValues(entry, colors, fonts, components, presentation);
        return new ResolvedTheme(id, manifest.name(), colors, assets, fonts, components, presentation);
    }
    private static Path asset(ThemeCatalog.Entry entry, String value) {
        try {
            Path relative = Path.of(value);
            if (value.isBlank() || relative.isAbsolute() || value.contains(":") || value.startsWith("\\") ||
                    Arrays.asList(value.replace('\\','/').split("/")).contains("..")) throw new IllegalArgumentException("Expected a theme-relative asset path");
            Path file = entry.root().resolve(relative).normalize().toRealPath();
            if (!file.startsWith(entry.root().toRealPath()) || !Files.isRegularFile(file)) throw new IllegalArgumentException("Asset escapes owning theme or is not a file");
            String lower = file.toString().toLowerCase(Locale.ROOT);
            if (!lower.matches(".*\\.(png|jpg|jpeg|atlas|fnt|ttf|otf)$")) throw new IllegalArgumentException("Unsupported presentation asset type");
            if (lower.endsWith(".atlas") || lower.endsWith(".fnt")) validatePages(entry, file);
            return file;
        } catch (Exception e) { throw invalid(entry, "asset " + value, e.getMessage()); }
    }
    private static void validatePages(ThemeCatalog.Entry entry, Path file) {
        if(file.toString().toLowerCase(Locale.ROOT).endsWith(".fnt"))bitmapFontData(file,entry.root());
        else atlasData(file,entry.root());
    }
    /** Use the native atlas page parser and canonical files, without loading a texture. */
    public static TextureAtlas.TextureAtlasData atlasData(Path file,Path root){
        try{
            TextureAtlas.TextureAtlasData data=new TextureAtlas.TextureAtlasData(new FileHandle(file.toFile()),new FileHandle(file.getParent().toFile()),false);
            if(data.getPages().isEmpty())throw new IllegalArgumentException("Atlas has no image pages");
            for(TextureAtlas.TextureAtlasData.Page page:data.getPages())
                page.textureFile=new FileHandle(imagePagePath(Path.of(page.textureFile.path()),root).toFile());
            return data;
        }catch(Exception e){throw new IllegalArgumentException(file+" [atlas pages]: "+e.getMessage(),e);}
    }
    private static Path imagePagePath(Path page,Path root) throws Exception {
        if(Arrays.asList(page.toString().replace((char)92,'/').split("/")).contains(".."))
            throw new IllegalArgumentException("Image page traverses its owning directory");
        Path real=page.toRealPath();
        if(!real.startsWith(root.toRealPath())||!Files.isRegularFile(real)||!real.toString().toLowerCase(Locale.ROOT).matches(".*[.](png|jpg|jpeg)$"))
            throw new IllegalArgumentException("Image page escapes its owning directory or has an unsupported type");
        return real;
    }
    /** Parse the descriptor consumed by libGDX without creating textures or an application. */
    public static BitmapFont.BitmapFontData bitmapFontData(Path file,Path root) {
        try {
            BitmapFont.BitmapFontData data=new BitmapFont.BitmapFontData(new FileHandle(file.toFile()),false);
            if(data.imagePaths==null||data.imagePaths.length==0)throw new IllegalArgumentException("Bitmap font has no image pages");
            for(int i=0;i<data.imagePaths.length;i++){
                String name=data.imagePaths[i];
                if(name==null||Arrays.asList(name.replace('\\','/').split("/")).contains(".."))throw new IllegalArgumentException("Bitmap font page traverses its owning directory");
                Path page=Path.of(name).toRealPath();
                if(!page.startsWith(root.toRealPath())||!Files.isRegularFile(page)||!page.toString().toLowerCase(Locale.ROOT).matches(".*\\.(png|jpg|jpeg)$"))
                    throw new IllegalArgumentException("Bitmap font image page escapes its owning directory or has an unsupported type");
                data.imagePaths[i]=page.toString();
            }
            return data;
        }catch(Exception e){throw new IllegalArgumentException(file+" [font pages]: "+e.getMessage(),e);}
    }
    private static void validateValues(ThemeCatalog.Entry entry, Map<String,String> colors, Map<String,String> fonts,
                                       Map<String,String> components, Map<String,String> presentation) {
        if (!colors.keySet().containsAll(ThemeManifest.COLOR_ROLES)) throw invalid(entry,"colors","Missing required semantic color roles");
        for (var color : colors.entrySet()) if (!ThemeManifest.COLOR_ROLES.contains(color.getKey()) || !color.getValue().matches("#[0-9a-fA-F]{6}"))
            throw invalid(entry,"colors."+color.getKey(),"Expected a known role and opaque #RRGGBB color");
        if (!fonts.keySet().containsAll(Set.of("body","heading"))) throw invalid(entry,"fonts","body and heading roles are required");
        for (String key : components.keySet()) if (!key.equals("shape")) throw invalid(entry,"components."+key,"Unknown component setting");
        if (!Set.of("round","angular","leaf").contains(components.getOrDefault("shape","round"))) throw invalid(entry,"components.shape","Expected round, angular, or leaf");
        for (String key : presentation.keySet()) if (!Set.of("ornament","paperTexture").contains(key)) throw invalid(entry,"presentation."+key,"Unknown presentation setting");
        if (!Set.of("quiet","balanced","full").contains(presentation.getOrDefault("ornament","full"))) throw invalid(entry,"presentation.ornament","Expected quiet, balanced, or full");
        try { float amount = Float.parseFloat(presentation.getOrDefault("paperTexture","0.16")); if (!Float.isFinite(amount) || amount < 0 || amount > .35f) throw new NumberFormatException(); }
        catch (NumberFormatException e) { throw invalid(entry,"presentation.paperTexture","Expected 0..0.35"); }
        String[][] pairs = {{"text.primary","surface.panel"},{"text.primary","surface.canvas"},{"text.secondary","surface.panel"},
                {"text.onPaper","surface.paper"},{"action.onPrimary","action.primary"},{"action.onSecondary","action.secondary"},
                {"state.onSelected","state.selected"},{"state.onDisabled","state.disabled"},{"border.frameText","border.frame"},{"text.primary","state.hover"},
                {"action.onPrimary","phase.activeEnabled"},{"text.onPaper","phase.activeDisabled"},
                {"text.primary","phase.inactiveEnabled"},{"text.primary","phase.inactiveDisabled"}};
        for (String[] pair : pairs) if (contrast(colors.get(pair[0]),colors.get(pair[1])) < 4.5)
            throw invalid(entry,"colors."+pair[0],"Needs contrast 4.5:1 against " + pair[1]);
        for (String boundary : List.of("border.frame","state.focus")) if (contrast(colors.get(boundary),colors.get("surface.panel")) < 3)
            throw invalid(entry,"colors."+boundary,"Needs contrast 3:1 against surface.panel");
    }
    public static double contrast(String a, String b) {
        double x=luminance(a), y=luminance(b);
        return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);
    }
    private static double luminance(String hex) {
        int color=Integer.parseInt(hex.substring(1),16); double result=0;
        double[] weights={.2126,.7152,.0722};
        for(int i=0;i<3;i++){double c=((color>>(16-8*i))&255)/255.0;result+=weights[i]*(c<=.04045?c/12.92:Math.pow((c+.055)/1.055,2.4));}
        return result;
    }
    private static IllegalArgumentException invalid(ThemeCatalog.Entry entry,String role,String message) {
        return new IllegalArgumentException(new ThemeDiagnostic(entry.file(),role,message).toString());
    }
}
