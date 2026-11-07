package forge.adventure.theme;

import java.nio.file.Path;

/** A presentation-only validation problem, suitable for the chooser and CLI. */
public record ThemeDiagnostic(Path file, String role, String message) {
    @Override public String toString() { return file + " [" + role + "]: " + message; }
}
