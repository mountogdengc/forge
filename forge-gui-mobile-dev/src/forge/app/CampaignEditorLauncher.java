package forge.app;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** Runs the authoring tools outside the game's process. */
final class CampaignEditorLauncher {
    private final Path root, javaHome, sourceEditor;
    private Process process;
    CampaignEditorLauncher(Path root, Path javaHome, Path sourceEditor) {
        this.root = root.toAbsolutePath().normalize();
        this.javaHome = javaHome;
        this.sourceEditor = sourceEditor;
    }
    List<String> command(String campaignId) throws IOException {
        Path editor = root.resolve("adventure-editor.jar");
        if (!Files.isRegularFile(editor) && sourceEditor != null) editor = sourceEditor;
        if (!Files.isRegularFile(editor)) throw new IOException("Campaign Editor is not installed. Install the Campaign Editor update.");
        String executable = System.getProperty("os.name").startsWith("Windows") ? "javaw.exe" : "java";
        Path java = root.resolve("runtime/bin/" + executable);
        if (!Files.isRegularFile(java)) java = javaHome.resolve("bin/" + executable);
        if (!Files.isRegularFile(java)) throw new IOException("The Java runtime for Campaign Editor is missing.");
        List<String> command = new ArrayList<>(List.of(java.toString(), "-Xms128m", "-Xmx512m", "-XX:ActiveProcessorCount=2",
                "-Duser.home=" + System.getProperty("user.home"), "-jar", editor.toString()));
        if (campaignId != null && !campaignId.isBlank()) command.addAll(List.of("--campaign", campaignId));
        return command;
    }
    synchronized String launch(String campaignId) {
        if (isRunning()) return "Campaign Editor is already open. Switch to its window to continue editing.";
        try {
            List<String> command = command(campaignId);
            Path logs = root.resolve("logs");
            Files.createDirectories(logs);
            process = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(logs.resolve("campaign-editor.log").toFile())).start();
            return null;
        } catch (IOException | SecurityException error) {
            return "Could not open Campaign Editor: " + error.getMessage();
        }
    }
    synchronized boolean isRunning() { return process != null && process.isAlive(); }
}
