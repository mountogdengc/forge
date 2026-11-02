package forge.adventure.editor;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import forge.adventure.campaign.CampaignRegistry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Creates an independent authoring project without activating game configuration. */
public final class CampaignProjectCreator {
    public record CreatedProject(Path root, String id) {}

    private CampaignProjectCreator() {}

    /** Copies the current map graph locally without changing campaign identity or story data.
     * @return the newly published private bundle directory
     */
    public static Path makeMapsPrivate(Path campaignRoot, Path commonRoot) throws IOException {
        Path campaign = checkedRoot(campaignRoot), common = checkedRoot(commonRoot);
        if (campaign.startsWith(common) || common.startsWith(campaign)) {
            throw new IOException("Campaign and common directories must be separate.");
        }
        Path catalog = campaign.resolve("world/points_of_interest.json");
        checkNoLinks(catalog);
        boolean local = Files.exists(catalog, LinkOption.NOFOLLOW_LINKS);
        Path sourceCatalog = checkedFile(local ? catalog : common.resolve("world/points_of_interest.json"), campaign, common);
        byte[] original = Files.readAllBytes(sourceCatalog);
        JsonValue points = json(sourceCatalog);
        if (!points.isArray()) throw new IOException("Location catalog must be an array.");
        Path staging = Files.createTempDirectory(campaign, ".campaign-create-");
        Object stagingKey = Files.readAttributes(staging, java.nio.file.attribute.BasicFileAttributes.class).fileKey();
        try {
            Path namespace = Path.of("maps", "private-" + UUID.randomUUID());
            MapCopier copier = new MapCopier(campaign, common, staging, namespace);
            Files.createDirectories(staging.resolve(namespace));
            for (JsonValue point = points.child; point != null; point = point.next) {
                String ref = point.getString("map", "");
                if (ref.isBlank()) throw new IOException("Every location needs a map.");
                copier.registerMap(copier.rootMap(ref));
            }
            for (JsonValue point = points.child; point != null; point = point.next) {
                Path destination = copier.copy(copier.rootMap(point.getString("map")), true);
                replace(point, "map", "campaign:" + slash(staging.relativize(destination)));
            }
            Path newCatalog = staging.resolve("points_of_interest.json");
            writeJson(newCatalog, points);
            verifyStaging(staging, campaign, stagingKey);
            Path maps = campaign.resolve("maps");
            checkNoLinks(maps);
            Files.createDirectories(maps);
            checkNoLinks(maps);
            Path bundle = campaign.resolve(namespace);
            // The bundle is a fresh namespace: never replace another authored map.
            publishDirectory(staging.resolve(namespace), bundle);
            checkNoLinks(catalog);
            if (local != Files.exists(catalog, LinkOption.NOFOLLOW_LINKS)
                    || !Arrays.equals(original, Files.readAllBytes(checkedFile(sourceCatalog, campaign, common)))) {
                throw new IOException("Location catalog changed while making maps private; retry the operation.");
            }
            Path world = catalog.getParent();
            checkNoLinks(world);
            Files.createDirectories(world);
            checkNoLinks(world);
            // An unsupported atomic replacement fails without modifying the old catalog.
            Files.move(newCatalog, catalog, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return bundle;
        } finally {
            cleanup(staging, campaign, stagingKey);
        }
    }

    public static CreatedProject create(Path templateRoot, Path commonRoot, Path projectsRoot,
                                        String title, String description) throws IOException {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("A campaign needs a title.");
        Path template = checkedRoot(templateRoot), common = checkedRoot(commonRoot);
        Path projects = projectsRoot.toAbsolutePath().normalize();
        checkNoLinks(projects);
        if (projects.startsWith(template) || projects.startsWith(common)) {
            throw new IOException("Projects must be outside the template and common directories.");
        }
        JsonValue manifest = json(template.resolve("campaign.json"));
        if (!manifest.isObject() || manifest.getInt("schemaVersion", 0) != 1
                || manifest.getBoolean("inheritCommonWorld", true)
                || manifest.getBoolean("inheritCommonQuests", true)) {
            throw new IOException("Choose an explicit campaign template with its own world and quests.");
        }
        for (String required : new String[]{"config.json", "world/world.json", "world/quests.json",
                "world/enemies.json", "world/points_of_interest.json"}) {
            Path file = checkedFile(template.resolve(required), template, common);
            JsonValue data = json(file);
            boolean array = required.equals("world/quests.json") || required.equals("world/enemies.json")
                    || required.equals("world/points_of_interest.json");
            if (array ? !data.isArray() : !data.isObject()) throw new IOException("Invalid template resource: " + required);
        }
        // Reject every source link before creating destination state, including unused authored files.
        try (var files = Files.walk(template)) {
            for (Path file : files.toList()) checkNoLinks(file);
        }
        Files.createDirectories(projects);
        checkNoLinks(projects);
        projects = projects.toRealPath();
        Path staging = Files.createTempDirectory(projects, ".campaign-create-");
        Object stagingKey = Files.readAttributes(staging, java.nio.file.attribute.BasicFileAttributes.class).fileKey();
        try {
            try (var files = Files.walk(template)) {
                for (Path source : files.toList()) {
                    Path destination = staging.resolve(template.relativize(source));
                    if (Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) Files.createDirectories(destination);
                    else {
                        checkedFile(source, template, common);
                        Files.copy(source, destination);
                    }
                }
            }
            String id = "user." + UUID.randomUUID();
            replace(manifest, "id", id);
            replace(manifest, "title", title.trim());
            replace(manifest, "description", description == null ? "" : description.trim());
            replace(manifest, "version", "0.1.0");
            writeJson(staging.resolve("campaign.json"), manifest);
            MapCopier copier = new MapCopier(template, common, staging);
            Path catalog = staging.resolve("world/points_of_interest.json");
            JsonValue points = json(catalog);
            if (!points.isArray()) throw new IOException("Location catalog must be an array.");
            for (JsonValue point = points.child; point != null; point = point.next) {
                String ref = point.getString("map", "");
                if (ref.isBlank()) throw new IOException("Every location needs a map.");
                Path source = copier.rootMap(ref);
                copier.registerMap(source);
            }
            for (JsonValue point = points.child; point != null; point = point.next) {
                Path destination = copier.copy(copier.rootMap(point.getString("map")), true);
                replace(point, "map", "campaign:" + slash(staging.relativize(destination)));
            }
            writeJson(catalog, points);
            CampaignRegistry.readProject(staging, common).validateResources();
            String folder = folderName(title);
            for (int attempt = 0; attempt < 100; attempt++) {
                String name = attempt == 0 ? folder : folder.substring(0, Math.min(folder.length(), 39))
                        + "-" + UUID.randomUUID().toString().substring(0, 8);
                Path destination = projects.resolve(name);
                try {
                    // No REPLACE_EXISTING or ATOMIC_MOVE: both could replace a racing destination.
                    publishDirectory(staging, destination);
                    return new CreatedProject(destination, id);
                } catch (FileAlreadyExistsException ignored) { /* choose a fresh destination */ }
            }
            throw new IOException("Cannot choose an unused campaign folder.");
        } finally {
            cleanup(staging, projects, stagingKey);
        }
    }

    private static final class MapCopier {
        private final Path template, common, staging, bundle;
        private final Map<Path, Path> destinations = new HashMap<>();
        private final Set<Path> visiting = new HashSet<>(), complete = new HashSet<>();

        private MapCopier(Path template, Path common, Path staging) {
            this(template, common, staging, Path.of(""));
        }

        private MapCopier(Path template, Path common, Path staging, Path namespace) {
            this.template = template; this.common = common; this.staging = staging;
            this.bundle = staging.resolve(namespace);
        }

        private Path rootMap(String ref) throws IOException {
            if (ref.startsWith("campaign:")) return checkedFile(template.resolve(relative(ref.substring(9))), template, common);
            Path relative = relative(ref);
            // Legacy root POI maps resolve against common, even when prefixed ../common/.
            String normalized = ref.replace('\\', '/');
            if (normalized.startsWith("../common/")) relative = Path.of(normalized.substring(10));
            return checkedFile(common.resolve(relative), template, common);
        }

        private void registerMap(Path source) {
            destinations.computeIfAbsent(source, p -> bundle.resolve("maps/locations")
                    .resolve(p.startsWith(template) ? "template" : "common")
                    .resolve((p.startsWith(template) ? template : common).relativize(p)));
        }

        private Path resource(String ref) throws IOException {
            if (ref.startsWith("campaign:")) return checkedFile(template.resolve(relative(ref.substring(9))), template, common);
            Path path = relative(ref);
            Path own = template.resolve(path).normalize();
            if (Files.exists(own, LinkOption.NOFOLLOW_LINKS)) return checkedFile(own, template, common);
            return checkedFile(common.resolve(path), template, common);
        }

        private Path copy(Path source, boolean map) throws IOException {
            checkedFile(source, template, common);
            if (map) registerMap(source);
            Path destination = destinations.computeIfAbsent(source, p -> bundle.resolve("assets/shared")
                    .resolve(p.startsWith(template) ? "template" : "common")
                    .resolve((p.startsWith(template) ? template : common).relativize(p)));
            if (complete.contains(source)) return destination;
            if (!visiting.add(source)) {
                // Map transitions form a graph: a return door may target a map still
                // being copied. Its private destination was reserved before traversal.
                // XML source/template recursion still indicates an invalid dependency.
                if (map) return destination;
                throw new IOException("Cyclic Tiled dependency: " + source);
            }
            try {
                Files.createDirectories(destination.getParent());
                String extension = source.getFileName().toString().toLowerCase(Locale.ROOT);
                if (extension.endsWith(".tmx") || extension.endsWith(".tsx") || extension.endsWith(".tx")) {
                    Document document = readXml(source);
                    NodeList nodes = document.getElementsByTagName("*");
                    for (int i = 0; i < nodes.getLength(); i++) {
                        Element element = (Element) nodes.item(i);
                        for (String attribute : new String[]{"source", "template"}) {
                            if (!element.hasAttribute(attribute) || element.getAttribute(attribute).isBlank()) continue;
                            Path dependency = checkedFile(source.getParent().resolve(relative(element.getAttribute(attribute))), template, common);
                            Path target = copy(dependency, false);
                            element.setAttribute(attribute, slash(destination.getParent().relativize(target)));
                        }
                        if (element.getTagName().equals("property") && element.getAttribute("name").equals("teleport")) {
                            String teleport = element.hasAttribute("value") ? element.getAttribute("value") : element.getTextContent();
                            if (!teleport.isBlank()) {
                                Path target = copy(resource(teleport), true);
                                String rewritten = slash(staging.relativize(target));
                                if (element.hasAttribute("value")) element.setAttribute("value", rewritten);
                                else element.setTextContent(rewritten);
                            }
                        }
                    }
                    writeXml(document, destination);
                } else Files.copy(source, destination);
                complete.add(source);
                return destination;
            } finally { visiting.remove(source); }
        }
    }

    /** Windows scanners may briefly hold freshly copied assets; retain no-overwrite semantics. */
    private static void publishDirectory(Path source, Path destination) throws IOException {
        for (int attempt = 0; ; attempt++) {
            try { Files.move(source, destination); return; }
            catch (java.nio.file.AccessDeniedException error) {
                if (attempt >= 9 || Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) throw error;
                try { Thread.sleep(100); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IOException("Campaign copy interrupted.", interrupted); }
            }
        }
    }

    private static Path relative(String value) throws IOException {
        if (value == null || value.isBlank() || value.indexOf(':') >= 0 || value.indexOf('\0') >= 0) throw new IOException("Invalid resource path: " + value);
        Path path = Path.of(value.replace('\\', '/'));
        if (path.isAbsolute()) throw new IOException("Absolute resource path: " + value);
        return path;
    }

    private static Path checkedRoot(Path root) throws IOException {
        Path absolute = root.toAbsolutePath().normalize();
        checkNoLinks(absolute);
        if (!Files.isDirectory(absolute, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Missing directory: " + absolute);
        return absolute.toRealPath();
    }

    private static Path checkedFile(Path file, Path template, Path common) throws IOException {
        Path normalized = file.toAbsolutePath().normalize();
        if (!normalized.startsWith(template) && !normalized.startsWith(common)) throw new IOException("Resource escapes template/common: " + file);
        checkNoLinks(normalized);
        if (!Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Missing resource: " + file);
        Path real = normalized.toRealPath();
        if (!real.equals(normalized)) throw new IOException("Linked resource: " + file);
        return real;
    }

    private static void checkNoLinks(Path path) throws IOException {
        for (Path part = path; part != null; part = part.getParent()) {
            if (Files.isSymbolicLink(part)) throw new IOException("Linked directories/files are not supported: " + part);
            if (Files.exists(part, LinkOption.NOFOLLOW_LINKS) && !part.toRealPath().equals(part.toAbsolutePath().normalize())) {
                throw new IOException("Linked directories/files are not supported: " + part);
            }
        }
    }

    private static Document readXml(Path path) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(path.toFile());
        } catch (Exception ex) { throw new IOException("Cannot safely read Tiled XML: " + path, ex); }
    }

    private static void writeXml(Document document, Path path) throws IOException {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            factory.newTransformer().transform(new DOMSource(document), new StreamResult(path.toFile()));
        } catch (Exception ex) { throw new IOException("Cannot write private Tiled XML: " + path, ex); }
    }

    private static JsonValue json(Path file) throws IOException {
        try { return new JsonReader().parse(Files.readString(file)); }
        catch (RuntimeException ex) { throw new IOException("Invalid campaign JSON: " + file, ex); }
    }

    private static void replace(JsonValue object, String key, String value) {
        object.remove(key); object.addChild(key, new JsonValue(value));
    }

    private static void writeJson(Path file, JsonValue value) throws IOException {
        Files.writeString(file, value.prettyPrint(JsonWriter.OutputType.json, 100));
    }

    private static String folderName(String title) {
        String folder = title.trim().replaceAll("[^A-Za-z0-9_-]+", "-").replaceAll("^-+|-+$", "");
        if (folder.isEmpty()) folder = "Campaign";
        if (folder.matches("(?i)CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]")) folder = "Campaign-" + folder;
        return folder.substring(0, Math.min(folder.length(), 48));
    }

    private static String slash(Path path) { return path.toString().replace('\\', '/'); }

    private static void cleanup(Path staging, Path parent, Object key) throws IOException {
        if (!Files.exists(staging, LinkOption.NOFOLLOW_LINKS)) return;
        verifyStaging(staging, parent, key);
        try (var files = Files.walk(staging)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(file);
        }
    }

    private static void verifyStaging(Path staging, Path parent, Object key) throws IOException {
        checkNoLinks(staging);
        if (!staging.getParent().equals(parent) || !staging.getFileName().toString().startsWith(".campaign-create-")
                || !java.util.Objects.equals(key, Files.readAttributes(staging, java.nio.file.attribute.BasicFileAttributes.class).fileKey())) {
            throw new IOException("Refusing to clean an unverified staging directory: " + staging);
        }
    }
}
