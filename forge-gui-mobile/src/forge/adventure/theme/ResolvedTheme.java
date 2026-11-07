package forge.adventure.theme;

import java.nio.file.Path;
import java.util.Map;

/** An immutable validated theme. Every asset retains its declaring directory. */
public record ResolvedTheme(String id, String name, Map<String,String> colors, Map<String,Path> assets,
                            Map<String,String> fonts, Map<String,String> components, Map<String,String> presentation) {
    public ResolvedTheme {
        colors = Map.copyOf(colors); assets = Map.copyOf(assets); fonts = Map.copyOf(fonts);
        components = Map.copyOf(components); presentation = Map.copyOf(presentation);
    }
    public String color(String role) {
        String value = colors.get(role);
        if (value == null) throw new IllegalArgumentException("Unknown theme color: " + role);
        return value;
    }
    public String shape() { return components.getOrDefault("shape", "round"); }
    public String ornament() { return presentation.getOrDefault("ornament", "full"); }
    public float texture() { return Float.parseFloat(presentation.getOrDefault("paperTexture", "0.16")); }
}
