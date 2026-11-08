package forge.adventure.character;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.github.tommyettinger.textra.TextraButton;
import forge.Forge;
import forge.adventure.util.Controls;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

/** Image browsing stays in Adventure's Scene2D input/rendering pipeline, including cancel and errors. */
public final class PortraitImportDialog extends Dialog {
    @FunctionalInterface public interface Importer { void accept(Path file) throws Exception; }
    public final TextField pathField = Controls.newTextField("");
    public final com.badlogic.gdx.scenes.scene2d.ui.List<String> files = new com.badlogic.gdx.scenes.scene2d.ui.List<>(Controls.getSkin());
    public final TextraButton up;
    public final TextraButton roots;
    private final Label status = Controls.newLabel("");
    private final java.util.List<Path> entries = new ArrayList<>();
    private final Importer importer;
    private final Runnable close;
    private Path directory;
    private boolean updating;

    public PortraitImportDialog(float screenWidth, float screenHeight, Importer importer, Runnable close) {
        super(text("lblImportImage", "Import image"), Controls.getSkin());
        this.importer = importer; this.close = close;
        pathField.setName("portraitPath"); files.setName("portraitFiles"); status.setName("portraitImportError"); status.setWrap(true);
        up = Controls.newTextButton(text("lblUp", "Up"), () -> { if (directory != null && directory.getParent() != null) browse(directory.getParent()); else showRoots(); });
        roots = Controls.newTextButton(text("lblDrives", "Locations"), this::showRoots);
        up.setName("portraitUp"); roots.setName("portraitLocations");
        float width = Math.max(190, screenWidth - 48);
        Table location = new Table(); location.add(pathField).expandX().fillX().height(20).padRight(4);
        location.add(up).width(35).height(20).padRight(4); location.add(roots).width(65).height(20);
        getContentTable().add(location).width(width).row();
        ScrollPane scrolling = new ScrollPane(files, Controls.getSkin()); scrolling.setScrollingDisabled(true, false);
        getContentTable().add(scrolling).width(width).height(Math.max(65, Math.min(160, screenHeight - 140))).padTop(5).row();
        getContentTable().add(status).width(width).height(28).padTop(3);
        TextraButton open = Controls.newTextButton(text("lblOpen", "Open"), this::openPath); open.setName("portraitOpen");
        TextraButton cancel = Controls.newTextButton(text("lblCancel", "Cancel"), close); cancel.setName("portraitCancel");
        getButtonTable().add(open).height(20).pad(2); getButtonTable().add(cancel).height(20).pad(2);
        files.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                int index = files.getSelectedIndex();
                if (!updating && index >= 0 && index < entries.size()) pathField.setText(entries.get(index).toString());
            }
        });
        files.addListener(new ClickListener() { @Override public void clicked(InputEvent event, float x, float y) {
            if (getTapCount() == 2) openPath();
        }});
        pathField.addListener(new InputListener() { @Override public boolean keyDown(InputEvent event, int keycode) {
            if (keycode == Input.Keys.ENTER) { openPath(); return true; } return false;
        }});
        Path home = Path.of(System.getProperty("user.home", ".")).toAbsolutePath().normalize();
        browse(Files.isDirectory(home.resolve("Pictures")) ? home.resolve("Pictures") : home);
    }
    private static String text(String key, String fallback) { return Forge.getLocalizer().getMessageorUseDefault(key, fallback); }
    private void browse(Path path) {
        try (var children = Files.list(path)) {
            java.util.List<Path> visible = children.filter(p -> Files.isDirectory(p) || imageName(p))
                    .sorted(Comparator.<Path, Boolean>comparing(p -> !Files.isDirectory(p))
                            .thenComparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT))).toList();
            directory = path.toAbsolutePath().normalize(); pathField.setText(directory.toString());
            setEntries(visible); status.setText("");
        } catch (Exception error) { status.setText(text("lblCannotOpenDirectory", "Cannot open this folder. Enter another path.")); }
    }
    private void showRoots() {
        java.io.File[] drives = java.io.File.listRoots(); directory = null;
        setEntries(drives == null ? java.util.List.of() : java.util.Arrays.stream(drives).map(java.io.File::toPath).toList());
        pathField.setText(""); status.setText("");
    }
    private void setEntries(java.util.List<Path> paths) {
        updating = true; entries.clear(); entries.addAll(paths);
        files.setItems(paths.stream().map(p -> (p.getFileName() == null ? p.toString() : p.getFileName().toString())
                + (Files.isDirectory(p) ? "/" : "")).toArray(String[]::new));
        updating = false;
    }
    public void moveSelection(int delta) {
        if (entries.isEmpty()) return;
        int index = Math.max(0, Math.min(entries.size() - 1, files.getSelectedIndex() + delta));
        files.setSelectedIndex(index); pathField.setText(entries.get(index).toString());
    }
    public void openSelection() {
        int index = files.getSelectedIndex();
        if (index >= 0 && index < entries.size()) pathField.setText(entries.get(index).toString());
        openPath();
    }
    private static boolean imageName(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
    }
    private void openPath() {
        try {
            Path path = Path.of(pathField.getText()).toAbsolutePath().normalize();
            if (Files.isDirectory(path)) browse(path);
            else { importer.accept(path); close.run(); }
        } catch (Exception error) { status.setText(error.getMessage() == null ? text("lblCannotOpenImage", "Cannot open this image.") : error.getMessage()); }
    }
}
