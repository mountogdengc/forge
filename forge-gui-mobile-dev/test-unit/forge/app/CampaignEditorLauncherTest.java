package forge.app;

import org.testng.annotations.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.tools.ToolProvider;
import static org.testng.Assert.*;

public class CampaignEditorLauncherTest {
    @Test public void bundledJavaAndEditorUseArgumentBoundariesAndResourceCaps() throws Exception {
        Path root = Files.createTempDirectory("campaign editor & space-");
        String javaName = System.getProperty("os.name").startsWith("Windows") ? "javaw.exe" : "java";
        Path java = root.resolve("runtime/bin/" + javaName);
        Files.createDirectories(java.getParent()); Files.writeString(java, "fixture");
        Files.writeString(root.resolve("adventure-editor.jar"), "fixture");
        var command = new CampaignEditorLauncher(root, Path.of(System.getProperty("java.home")), null).command("fork.curse-of-strahd");
        assertTrue(command.size() > 6, "Missing editor launch command");
        assertEquals(command.get(0), java.toString());
        assertTrue(command.contains("-Xmx512m") && command.contains("-XX:ActiveProcessorCount=2"));
        assertEquals(command.get(command.size()-1), "fork.curse-of-strahd");
        assertTrue(command.contains(root.resolve("adventure-editor.jar").toString()));
    }

    @Test public void missingEditorGivesAnActionableError() throws Exception {
        Path root = Files.createTempDirectory("missing-campaign-editor-");
        String error = new CampaignEditorLauncher(root, Path.of(System.getProperty("java.home")), null).launch("fork.bellwake");
        assertNotNull(error);
        assertTrue(error.contains("not installed") && error.contains("update"), error);
    }

    @Test public void launchesOneIndependentChildWithoutChangingGameFiles() throws Exception {
        Path root = Files.createTempDirectory("campaign editor child-");
        Files.writeString(root.resolve("keep.save"), "unchanged");
        Path source = root.resolve("EditorFixture.java");
        Files.writeString(source, "public class EditorFixture { public static void main(String[] a) throws Exception {"
                + "java.nio.file.Files.writeString(java.nio.file.Path.of(\"editor-args.txt\"),String.join(\"|\",a));"
                + "Thread.sleep(1200); }}");
        assertEquals(ToolProvider.getSystemJavaCompiler().run(null,null,null,"--release","17","-d",root.toString(),source.toString()),0);
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION,"1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS,"EditorFixture");
        try (var jar = new JarOutputStream(Files.newOutputStream(root.resolve("adventure-editor.jar")), manifest)) {
            jar.putNextEntry(new JarEntry("EditorFixture.class")); jar.write(Files.readAllBytes(root.resolve("EditorFixture.class"))); jar.closeEntry();
        }
        var launcher = new CampaignEditorLauncher(root,Path.of(System.getProperty("java.home")),null);
        assertNull(launcher.launch("fork.curse-of-strahd"));
        assertTrue(launcher.isRunning(), "No editor child started");
        assertNotNull(launcher.launch("fork.curse-of-strahd"), "Repeated clicks launched duplicate editors");
        long deadline = System.nanoTime()+15_000_000_000L;
        while (launcher.isRunning() && System.nanoTime()<deadline) Thread.sleep(20);
        assertFalse(launcher.isRunning(), "Editor child did not close independently");
        assertEquals(Files.readString(root.resolve("editor-args.txt")), "--campaign|fork.curse-of-strahd");
        assertEquals(Files.readString(root.resolve("keep.save")), "unchanged");
    }
}
