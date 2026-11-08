package forge.adventure.character;

import forge.adventure.data.CharacterCreationData;
import forge.adventure.data.CharacterCreationData.*;
import forge.adventure.data.HeroData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Texture-free catalog/policy; previewing a campaign never changes the active campaign. */
public final class CharacterCreationCatalog {
    public final CharacterCreationData settings;
    public final List<Gender> genders;
    public final List<Race> races;
    public final List<Portrait> portraits;
    public final List<Appearance> appearances;
    public final List<StarterClass> starterClasses;

    public CharacterCreationCatalog(CharacterCreationData settings, HeroData[] heroes, List<Portrait> inheritedPortraits) {
        this.settings = settings == null ? new CharacterCreationData() : settings;
        genders = options(this.settings.genders, defaultGenders(), "genders", true);
        List<Race> inheritedRaces = new ArrayList<>();
        List<Appearance> inheritedAppearances = new ArrayList<>();
        for (int i = 0; i < heroes.length; i++) {
            HeroData hero = heroes[i];
            Race race = new Race(); race.id = id(hero.name); race.label = hero.name;
            inheritedRaces.add(race);
            addAppearance(inheritedAppearances, hero.male, i, false, race.id);
            addAppearance(inheritedAppearances, hero.female, i, true, race.id);
        }
        races = options(this.settings.races, inheritedRaces, "races", true);
        portraits = options(this.settings.portraits, inheritedPortraits, "portraits", false);
        appearances = options(this.settings.appearances, inheritedAppearances, "appearances", true);
        starterClasses = options(this.settings.starterClasses, List.of(), "starterClasses", false);
    }

    public List<Portrait> matchingPortraits(String raceId, String genderId, boolean matchRace, boolean matchGender) {
        return portraits.stream().filter(p -> !matchRace || matches(p.races, raceId))
                .filter(p -> !matchGender || "unspecified".equals(genderId) || matches(p.genders, genderId)).toList();
    }

    private static boolean matches(String[] tags, String id) {
        if (tags == null || tags.length == 0) return true;
        for (String tag : tags) if ("*".equals(tag) || id.equals(tag)) return true;
        return false;
    }

    public static String id(String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    private static List<Gender> defaultGenders() {
        List<Gender> values = new ArrayList<>();
        for (String name : new String[]{"Unspecified", "Female", "Male", "Nonbinary"}) {
            Gender gender = new Gender(); gender.id = id(name); gender.label = name;
            gender.namePool = name.equals("Female") || name.equals("Male") ? name : "Any";
            values.add(gender);
        }
        return values;
    }

    private static void addAppearance(List<Appearance> list, String sprite, int index, boolean female, String race) {
        if (sprite == null || sprite.isEmpty()) return;
        if (list.stream().anyMatch(value -> value.sprite.equals(sprite))) return;
        Appearance appearance = new Appearance();
        appearance.id = race + (female ? "-f" : "-m"); appearance.sprite = sprite;
        appearance.heroIndex = index; appearance.female = female;
        list.add(appearance);
    }

    private static <T extends Option> List<T> options(T[] configured, List<T> inherited, String name, boolean required) {
        List<T> result = configured == null ? inherited : java.util.Arrays.asList(configured);
        if (required && result.isEmpty()) throw new IllegalArgumentException("Character creation needs " + name + ".");
        Set<String> ids = new HashSet<>();
        for (T option : result) {
            if (option == null || option.id == null || option.id.isBlank() || !ids.add(option.id))
                throw new IllegalArgumentException("Character creation " + name + " need unique, nonempty IDs.");
        }
        return List.copyOf(result);
    }
}
