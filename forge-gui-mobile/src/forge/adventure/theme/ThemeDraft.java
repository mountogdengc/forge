package forge.adventure.theme;

import com.google.gson.JsonObject;
import java.util.*;

/** Editable recipe data; never changes the active runtime or the source manifest. */
public final class ThemeDraft {
    private final ThemeCatalog catalog;
    private final String baseId,previewId="user.preview."+UUID.randomUUID();
    private final ResolvedTheme base;
    private final Map<String,String> colors=new LinkedHashMap<>(),presentation=new LinkedHashMap<>();
    private String name;
    private ThemeDraft(ThemeCatalog catalog,String baseId){this.catalog=catalog;this.baseId=baseId;this.base=ThemeResolver.resolve(catalog,baseId);restore();}
    public static ThemeDraft from(ThemeCatalog catalog,String baseId){return new ThemeDraft(catalog,baseId);}
    public String baseId(){return baseId;}
    public String name(){return name;}
    public String color(String role){return colors.getOrDefault(role,base.color(role));}
    public String ornament(){return presentation.getOrDefault("ornament",base.ornament());}
    public float texture(){return Float.parseFloat(presentation.getOrDefault("paperTexture",Float.toString(base.texture())));}
    public void setName(String value){name=value==null?"":value.trim();}
    public void setColor(String role,String value){
        if(!ThemeManifest.COLOR_ROLES.contains(role))throw new IllegalArgumentException("Unknown color role: "+role);
        value=value==null?"":value.trim().toLowerCase(Locale.ROOT);
        if(value.equalsIgnoreCase(base.color(role)))colors.remove(role);else colors.put(role,value);
    }
    public void setOrnament(String value){if(Objects.equals(value,base.ornament()))presentation.remove("ornament");else presentation.put("ornament",value);}
    public void setTexture(float value){if(value==base.texture())presentation.remove("paperTexture");else presentation.put("paperTexture",Float.toString(value));}
    public void restore(){colors.clear();presentation.clear();name=base.name();}
    public ThemeManifest toVariant(String id){
        JsonObject doc=new JsonObject();doc.addProperty("schemaVersion",1);doc.addProperty("id",id);doc.addProperty("name",name);doc.addProperty("extends",baseId);
        if(!colors.isEmpty()){JsonObject section=new JsonObject();colors.forEach(section::addProperty);doc.add("colors",section);}
        if(!presentation.isEmpty()){
            JsonObject section=new JsonObject();presentation.forEach((key,value)->{if(key.equals("paperTexture"))section.addProperty(key,Float.parseFloat(value));else section.addProperty(key,value);});
            doc.add("presentation",section);
        }
        return ThemeManifest.parse(catalog.entry(baseId).file(),doc.toString());
    }
    public ResolvedTheme resolve(){return ThemeResolver.resolve(catalog.withManifest(catalog.entry(baseId).root(),toVariant(previewId)),previewId);}
    public List<String> validate(){try{resolve();return List.of();}catch(IllegalArgumentException e){return List.of(e.getMessage());}}
}
