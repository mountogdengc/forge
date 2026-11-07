package forge.adventure.theme;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/** Writes only appearance settings and newly owned theme directories. */
public final class ThemeStore {
    private ThemeStore() { }
    public static Path saveAsNew(Path userRoot, ThemeCatalog catalog, ThemeManifest variant) {
        String id="user."+UUID.randomUUID();
        JsonObject json=variant.json();json.addProperty("id",id);
        ThemeManifest renamed=ThemeManifest.parse(userRoot.resolve(id).resolve("theme.json"),json.toString());
        Path directory=userRoot.toAbsolutePath().normalize().resolve(id);
        // Validate before creating a directory; malformed recipes leave no published entry.
        ThemeResolver.resolve(catalog.withManifest(directory,renamed),id);
        Path temp=null;
        try {
            Files.createDirectories(userRoot);
            Files.createDirectory(directory);
            temp=Files.createTempFile(directory,"recipe-",".tmp");
            Files.writeString(temp,renamed.toJson());
            Path target=directory.resolve("theme.json");
            Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);
            return target;
        } catch (IOException e) {
            try { if(temp!=null)Files.deleteIfExists(temp);Files.deleteIfExists(directory); } catch(IOException cleanup){e.addSuppressed(cleanup);}
            throw new IllegalArgumentException("Cannot save new theme in "+directory+": "+e.getMessage(),e);
        }
    }
    public static String readSelection(Path settings) {
        if(!Files.exists(settings))return null;
        try {
            // Existing Forge settings may use the older minimal JSON writer.
            JsonObject doc=JsonParser.parseString(Files.readString(settings)).getAsJsonObject();
            JsonElement selected=doc.get("adventureThemeId");
            return selected==null||selected.isJsonNull()?null:selected.getAsString();
        }catch(Exception e){throw new IllegalArgumentException("Cannot read Adventure appearance from "+settings+": "+e.getMessage(),e);}
    }
    public static void writeSelection(Path settings,String id) {
        if(id==null||!id.matches("[a-z][a-z0-9._-]{0,79}"))throw new IllegalArgumentException("Invalid appearance id");
        Path temp=null;
        try {
            JsonObject doc=Files.exists(settings)?JsonParser.parseString(Files.readString(settings)).getAsJsonObject():new JsonObject();
            doc.addProperty("adventureThemeId",id);
            Path parent=settings.toAbsolutePath().getParent();Files.createDirectories(parent);
            temp=Files.createTempFile(parent,"appearance-",".tmp");
            Files.writeString(temp,new GsonBuilder().setPrettyPrinting().create().toJson(doc));
            Files.move(temp,settings.toAbsolutePath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        }catch(Exception e){
            try{if(temp!=null)Files.deleteIfExists(temp);}catch(IOException cleanup){e.addSuppressed(cleanup);}
            throw new IllegalArgumentException("Cannot save Adventure appearance to "+settings+": "+e.getMessage(),e);
        }
    }
}
