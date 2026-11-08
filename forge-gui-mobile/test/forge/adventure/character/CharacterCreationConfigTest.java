package forge.adventure.character;

import com.badlogic.gdx.utils.Json;
import forge.adventure.data.ConfigData;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class CharacterCreationConfigTest {
    @Test
    public void campaignCreationSettingsRoundTripAlongsideExistingStarterPolicy() {
        Json json = new Json();
        ConfigData config = json.fromJson(ConfigData.class, """
                {allowedStarterModes:[Constructed], characterCreation:{
                  allowPortraitImport:true, matchPortraitRace:false, matchPortraitGender:false,
                  genders:[{id:nonbinary,label:Nonbinary,namePool:Any}],
                  portraits:[{id:traveler,image:portraits/traveler.png,races:[human],genders:[nonbinary]}],
                  starterClasses:[{id:paladin,label:Paladin,deck:decks/my-paladin.dck,color:W}]
                }}
                """);
        assertTrue(config.allowsStarterMode("Constructed"));
        assertFalse(config.allowsStarterMode("Chaos"));
        String saved = json.toJson(config);
        assertTrue(saved.contains("nonbinary"));
        assertTrue(saved.contains("decks/my-paladin.dck"));
        assertTrue(saved.contains("portraits/traveler.png"));
    }
}
