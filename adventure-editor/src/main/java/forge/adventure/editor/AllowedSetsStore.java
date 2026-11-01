package forge.adventure.editor;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;

/** Edit only the authored set policy, preserving even fields unknown to this editor version. */
final class AllowedSetsStore {
    private AllowedSetsStore() {}

    static void save(Path config, Collection<String> codes) throws IOException {
        JsonValue document = new JsonReader().parse(Files.readString(config));
        if (!document.isObject()) throw new IllegalArgumentException("Campaign configuration must be an object.");
        JsonValue editions = new JsonValue(JsonValue.ValueType.array);
        for (String code : codes) editions.addChild(new JsonValue(code));
        document.remove("allowedEditions");
        // Some older campaign consumers distinguish [] from null. Removing the whitelist
        // restores the same unrestricted semantics as an originally unconfigured campaign.
        if (!codes.isEmpty()) document.addChild("allowedEditions", editions);
        Path destination = config.toAbsolutePath();
        Path temporary = Files.createTempFile(destination.getParent(), ".allowed-sets-", ".json");
        try {
            Files.writeString(temporary, document.prettyPrint(JsonWriter.OutputType.json, 100), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
