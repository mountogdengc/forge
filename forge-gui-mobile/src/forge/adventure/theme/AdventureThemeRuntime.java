package forge.adventure.theme;

import forge.adventure.util.Config;
import forge.localinstance.properties.ForgeConstants;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/** Active recipes are process snapshots; only isolated previews use newly edited recipes. */
public final class AdventureThemeRuntime {
    private static AdventureThemeRuntime process;
    private final Path builtins, users;
    private final String startupId;
    private String activeId, pendingId;
    private final ThemeCatalog startupCatalog;
    private final Consumer<String> persist;
    private final Map<String,ResolvedTheme> resolved=new HashMap<>();
    private final Map<Config,Boolean> bespoke=new WeakHashMap<>();
    private final Map<Config,String> incompatible=new WeakHashMap<>();
    private final Set<String> problems=new LinkedHashSet<>();
    private final Set<String> rejected=new HashSet<>();

    public AdventureThemeRuntime(Path builtins,Path users,String startupId,Consumer<String> persist) {
        this.builtins=builtins;this.users=users;this.startupId=startupId;this.pendingId=startupId;this.persist=persist;
        this.startupCatalog=ThemeCatalog.snapshot(builtins,users);
        startupCatalog.diagnostics().forEach(d->problems.add(d.toString()));
    }
    public static synchronized void initialize(Path builtins,Path users,String startupId) {
        if(process!=null)return;
        Path settings=Path.of(ForgeConstants.USER_ADVENTURE_DIR,"settings.json");
        process=new AdventureThemeRuntime(builtins,users,startupId,id->{
            ThemeStore.writeSelection(settings,id);
            // Keep future ordinary settings saves from overwriting this pending selection.
            Config.instance().getSettingData().adventureThemeId=id;
        });
    }
    public static synchronized void initializeFromProfile() {
        if(process!=null)return;
        Path settings=Path.of(ForgeConstants.USER_ADVENTURE_DIR,"settings.json");
        String id=null;String problem=null;
        try{id=ThemeStore.readSelection(settings);}catch(IllegalArgumentException e){problem=e.getMessage();}
        initialize(Path.of(ForgeConstants.ADVENTURE_COMMON_DIR,"themes"),Path.of(ForgeConstants.USER_ADVENTURE_DIR,"themes"),id);
        if(problem!=null)process.problems.add(problem);
    }
    public static AdventureThemeRuntime current() { return process; }
    public ResolvedTheme resolveActive(Config config) {
        if(incompatible.containsKey(config)){activeId="campaign";ThemePaletteBridge.activate(null);return null;}
        ResolvedTheme theme=resolveActive(bespoke.computeIfAbsent(config,Config::hasCustomThemeSkin));
        ThemePaletteBridge.activate(theme);
        return theme;
    }
    public ResolvedTheme resolveActive(boolean bespokeCampaign) {
        String id=startupId==null||startupId.isBlank()?(bespokeCampaign?"campaign":"folio"):startupId;
        if(id.equals("campaign")){activeId="campaign";return null;}
        try {
            if(rejected.contains(id))throw new IllegalArgumentException("Theme '"+id+"' has unavailable native resources");
            ResolvedTheme theme=resolved.computeIfAbsent(id,k->ThemeResolver.resolve(startupCatalog,k));
            activeId=theme.id();return theme;
        } catch(IllegalArgumentException e) {
            problems.add(e.getMessage()+"; using Folio where available.");
            try{if(rejected.contains("folio"))throw new IllegalArgumentException("Folio has unavailable native resources");
                ResolvedTheme fallback=resolved.computeIfAbsent("folio",k->ThemeResolver.resolve(startupCatalog,k));activeId="folio";return fallback;}
            catch(IllegalArgumentException missing){problems.add(missing.getMessage()+"; preserving existing campaign appearance.");activeId="campaign";return null;}
        }
    }
    public String activeId() { return activeId; }
    public String pendingId() { return pendingId; }
    public Path userRoot() { return users; }
    public ThemeCatalog discover() { return ThemeCatalog.snapshot(builtins,users); }
    public void requestSelection(String id) {
        if(!"campaign".equals(id))ThemeResolver.resolve(discover(),id);
        persist.accept(id);pendingId=id;
    }
    public List<String> diagnostics() { return List.copyOf(problems); }
    public void reportCompatibility(String message) { problems.add(message); }
    public void useCampaignPresentation(Config config,String reason){
        incompatible.put(config,reason);problems.add(reason);activeId="campaign";ThemePaletteBridge.activate(null);
    }
    public void rejectTheme(String id,String reason) {
        rejected.add(id);resolved.remove(id);problems.add("Theme '"+id+"': "+reason);
    }
}
