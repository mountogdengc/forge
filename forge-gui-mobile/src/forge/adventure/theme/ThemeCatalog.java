package forge.adventure.theme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/** Captures manifest contents, never retaining mutable JSON supplied by callers. */
public final class ThemeCatalog {
    record Entry(Path file, Path root, ThemeManifest manifest) { }
    private final Map<String,Entry> entries;
    private final List<ThemeDiagnostic> diagnostics;
    private ThemeCatalog(Map<String,Entry> entries, List<ThemeDiagnostic> diagnostics) {
        this.entries = Map.copyOf(entries); this.diagnostics = List.copyOf(diagnostics);
    }
    public static ThemeCatalog snapshot(Path builtInRoot, Path userRoot) {
        Map<String,Entry> entries = new LinkedHashMap<>(); List<ThemeDiagnostic> errors = new ArrayList<>(); Set<String> duplicates = new HashSet<>();
        scan(builtInRoot, entries, errors, duplicates); scan(userRoot, entries, errors, duplicates);
        if (!entries.containsKey("folio")) errors.add(new ThemeDiagnostic(builtInRoot, "folio", "Required built-in Folio is missing or invalid"));
        return new ThemeCatalog(entries, errors);
    }
    private static void scan(Path directory, Map<String,Entry> entries, List<ThemeDiagnostic> errors, Set<String> duplicates) {
        if (directory == null || !Files.isDirectory(directory)) return;
        try (Stream<Path> children = Files.list(directory)) {
            Path realDirectory = directory.toRealPath();
            for (Path child : children.sorted().toList()) {
                Path file = child.resolve("theme.json");
                if (!Files.isDirectory(child) || !Files.exists(file)) continue;
                try {
                    Path root = child.toRealPath();
                    if (!root.startsWith(realDirectory) || !file.toRealPath().startsWith(root)) throw new IllegalArgumentException("Theme directory escapes catalog root");
                    ThemeManifest manifest = ThemeManifest.parse(file, Files.readString(file));
                    if (duplicates.contains(manifest.id()) || entries.containsKey(manifest.id())) {
                        duplicates.add(manifest.id()); entries.remove(manifest.id());
                        errors.add(new ThemeDiagnostic(file, "id", "Duplicate theme id: " + manifest.id()));
                    } else entries.put(manifest.id(), new Entry(file, root, manifest));
                } catch (Exception e) { errors.add(new ThemeDiagnostic(file, "manifest", e.getMessage())); }
            }
        } catch (Exception e) { errors.add(new ThemeDiagnostic(directory, "catalog", e.getMessage())); }
    }
    Entry entry(String id) { return entries.get(id); }
    public List<ThemeDiagnostic> diagnostics() { return diagnostics; }
    public List<ThemeManifest> themes() { return entries.values().stream().map(Entry::manifest).sorted(Comparator.comparing(ThemeManifest::name).thenComparing(ThemeManifest::id)).toList(); }
    public ThemeCatalog withManifest(Path directory, ThemeManifest manifest) {
        Map<String,Entry> copy = new LinkedHashMap<>(entries);
        copy.put(manifest.id(), new Entry(directory.resolve("theme.json"), directory.toAbsolutePath().normalize(), manifest));
        return new ThemeCatalog(copy, diagnostics);
    }
}
