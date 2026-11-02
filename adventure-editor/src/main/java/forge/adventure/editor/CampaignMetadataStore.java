package forge.adventure.editor;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

/** Updates the human-facing descriptor without changing campaign identity. */
final class CampaignMetadataStore {
    private CampaignMetadataStore() { }
    static void save(forge.adventure.campaign.CampaignDefinition campaign, String title, String description, String version) throws IOException {
        if (Files.exists(campaign.getRoot().resolve("campaign.json"))) {
            save(campaign.getRoot(), title, description, version); return;
        }
        if (title == null || title.isBlank() || version == null || !version.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"))
            throw new IllegalArgumentException("Enter a title and a valid version.");
        JsonValue document = new JsonValue(JsonValue.ValueType.object);
        document.addChild("schemaVersion", new JsonValue(1));
        document.addChild("id", new JsonValue(campaign.getId()));
        document.addChild("version", new JsonValue(version));
        document.addChild("title", new JsonValue(title.trim()));
        document.addChild("description", new JsonValue(description == null ? "" : description.trim()));
        document.addChild("inheritCommonWorld", new JsonValue(true));
        document.addChild("inheritCommonQuests", new JsonValue(true));
        document.addChild("development", new JsonValue(campaign.isDevelopment()));
        document.addChild("allowNewGamePlus", new JsonValue(campaign.isNewGamePlusAllowed()));
        if (campaign.getStartingLocation() != null) document.addChild("startingLocation", new JsonValue(campaign.getStartingLocation()));
        JsonValue quests = new JsonValue(JsonValue.ValueType.array);
        for (String quest : campaign.getStartingQuestIds()) quests.addChild(new JsonValue(quest));
        document.addChild("startingQuestIds", quests);
        Files.writeString(campaign.getRoot().resolve("campaign.json"), document.prettyPrint(JsonWriter.OutputType.json,100), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }
    static void save(Path root, String title, String description, String version) throws IOException {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("A campaign needs a title.");
        if (version == null || !version.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,127}"))
            throw new IllegalArgumentException("Version must use letters, numbers, dots, underscores or hyphens.");
        Path destination = root.resolve("campaign.json").toAbsolutePath();
        JsonValue document = new JsonReader().parse(Files.readString(destination));
        if (!document.isObject()) throw new IllegalArgumentException("Campaign descriptor must be an object.");
        document.remove("title"); document.addChild("title", new JsonValue(title.trim()));
        document.remove("description"); document.addChild("description", new JsonValue(description == null ? "" : description.trim()));
        document.remove("version"); document.addChild("version", new JsonValue(version));
        Path temporary = Files.createTempFile(destination.getParent(), ".campaign-details-", ".json");
        try {
            Files.writeString(temporary, document.prettyPrint(JsonWriter.OutputType.json, 100), StandardCharsets.UTF_8);
            try { Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
