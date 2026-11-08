package forge.adventure.data;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.ObjectMap;
import forge.Forge;
import forge.adventure.util.Config;
import forge.adventure.util.Paths;

/**
 * Data class that will be used to read Json configuration files
 * BiomeData
 * contains the a list of all heroes
 */
public class HeroListData {
    static private HeroListData instance;
    private HeroData[] heroes;
    private String avatar;
    private TextureAtlas avatarAtlas;
    private final ObjectMap<String, Array<Sprite>> avatarSprites = new ObjectMap<>();
    private static Config activeConfig;

    static public HeroListData instance() {
        if (instance == null || activeConfig != Config.instance()) {
            instance = forConfig(Config.instance());
            activeConfig = Config.instance();
        }
        return instance;
    }

    /** Character creation may preview another campaign without activating it. */
    public static HeroListData forConfig(Config config) {
        FileHandle handle = config.getFile(Paths.HEROES);
        if (handle == null || !handle.exists()) {
            throw new IllegalArgumentException("Campaign hero data is missing.");
        }
        HeroListData heroes = new Json().fromJson(HeroListData.class, handle);
        heroes.avatarAtlas = config.getAtlas(heroes.avatar);
        return heroes;
    }

    public String getHero(int raceIndex, boolean female) {
        HeroData data = heroes[raceIndex];

        if (female)
            return data.female;
        return data.male;

    }

    public TextureRegion getAvatar(int heroRace, boolean isFemale, int avatarIndex) {
        HeroData data = heroes[heroRace];
        Array<Sprite> sprites = avatarSprites.get(isFemale ? data.femaleAvatar : data.maleAvatar);
        if (sprites == null) {
            sprites = avatarAtlas.createSprites(isFemale ? data.femaleAvatar : data.maleAvatar);
            avatarSprites.put(isFemale ? data.femaleAvatar : data.maleAvatar, sprites);
        }
        avatarIndex %= sprites.size;
        if (avatarIndex < 0) {
            avatarIndex += sprites.size;
        }
        return sprites.get(avatarIndex);
    }

    public Array<String> getRaces() {
        Array<String> ret = new Array<>();
        for (HeroData hero : heroes) {
            ret.add(Forge.getLocalizer().getMessageorUseDefault("lbl"+hero.name, hero.name));
        }
        return ret;
    }

    public HeroData[] getHeroes() {
        return heroes.clone();
    }

    /** Legacy avatars become independent choices; their shared atlas remains owned by the asset manager. */
    public java.util.List<CharacterCreationData.Portrait> creationPortraits() {
        java.util.List<CharacterCreationData.Portrait> result = new java.util.ArrayList<>();
        for (int i = 0; i < heroes.length; i++) {
            HeroData hero = heroes[i];
            for (boolean female : new boolean[]{false, true}) {
                String region = female ? hero.femaleAvatar : hero.maleAvatar;
                int count = region == null ? 0 : avatarAtlas.createSprites(region).size;
                for (int index = 0; index < count; index++) {
                    CharacterCreationData.Portrait portrait = new CharacterCreationData.Portrait();
                    String race = forge.adventure.character.CharacterCreationCatalog.id(hero.name);
                    portrait.id = race + (female ? "-f-" : "-m-") + index;
                    portrait.label = hero.name;
                    portrait.races = new String[]{race};
                    portrait.genders = new String[]{female ? "female" : "male"};
                    portrait.legacy = true; portrait.heroIndex = i; portrait.female = female; portrait.avatarIndex = index;
                    result.add(portrait);
                }
            }
        }
        return result;
    }
}
