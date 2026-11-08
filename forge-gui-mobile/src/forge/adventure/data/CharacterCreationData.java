package forge.adventure.data;

/** Optional campaign authoring settings. Null collections inherit the legacy hero/deck catalogs. */
public class CharacterCreationData {
    public Gender[] genders;
    public Race[] races;
    public Portrait[] portraits;
    public Appearance[] appearances;
    public StarterClass[] starterClasses;
    public boolean allowPortraitImport = true;
    public boolean matchPortraitRace;
    public boolean matchPortraitGender;

    public static class Option {
        public String id = "";
        public String label = "";
    }

    public static class Gender extends Option {
        public String namePool = "Any";
    }

    public static class Race extends Option { }

    public static class Portrait extends Option {
        public String image = "";
        public int x, y, width, height;
        public String[] races = new String[0];
        public String[] genders = new String[0];
        public boolean legacy;
        public int heroIndex, avatarIndex;
        public boolean female;
    }

    public static class Appearance extends Option {
        public String sprite = "";
        public int heroIndex;
        public boolean female;
    }

    public static class StarterClass extends Option {
        public String deck = "";
        public String color = "";
        public String mode = "Constructed";
    }
}
