package forge.adventure.theme;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Validate recipes without creating a game, profile, card database, or renderer. */
public final class ThemeValidationMain {
    private ThemeValidationMain() { }
    public static void main(String[] args) {
        int result = run(args);
        if (result != 0) System.exit(result);
    }
    public static int run(String[] args) {
        try {
            Map<String,String> options = new HashMap<>();
            if (args.length % 2 != 0) throw new IllegalArgumentException("Use --builtins <root> --users <root> --theme <id>");
            for (int i=0;i<args.length;i+=2) {
                if (!java.util.Set.of("--builtins","--users","--theme").contains(args[i]) || options.put(args[i],args[i+1])!=null)
                    throw new IllegalArgumentException("Unknown or repeated option: "+args[i]);
            }
            if (!options.containsKey("--builtins") || !options.containsKey("--theme")) throw new IllegalArgumentException("--builtins and --theme are required");
            ThemeCatalog catalog = ThemeCatalog.snapshot(Path.of(options.get("--builtins")),options.containsKey("--users")?Path.of(options.get("--users")):null);
            ResolvedTheme result = ThemeResolver.resolve(catalog,options.get("--theme"));
            System.out.println("VALID " + result.id() + " (" + result.colors().size() + " colors, " + result.assets().size() + " assets)");
            return 0;
        } catch (Exception e) { System.err.println("INVALID " + e.getMessage()); return 1; }
    }
}
