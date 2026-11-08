package forge.adventure.character;

import forge.adventure.data.CharacterCreationData.Portrait;
import forge.adventure.util.SaveFileData;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class CharacterIdentityTest {
    @Test public void independentIdentityAndImportedBytesRoundTripWithoutTheOriginalFile() {
        CharacterIdentity identity = new CharacterIdentity();
        identity.genderId = "nonbinary"; identity.genderLabel = "Nonbinary";
        identity.raceId = "elf"; identity.raceLabel = "Elf";
        identity.appearanceId = "traveler"; identity.appearanceSprite = "sprites/human-f.atlas";
        identity.starterClassId = "paladin";
        identity.portrait = new Portrait(); identity.portrait.id = "custom";
        identity.portraitPng = new byte[]{1, 2, 3};
        SaveFileData saved = identity.toSaveData();
        identity.portraitPng[0] = 99;
        CharacterIdentity restored = CharacterIdentity.fromSaveData(saved);
        assertNotNull(restored, "A new character must restore its independent identity.");
        assertEquals(restored.genderId, "nonbinary"); assertEquals(restored.raceId, "elf");
        assertEquals(restored.appearanceSprite, "sprites/human-f.atlas");
        assertEquals(restored.starterClassId, "paladin");
        assertEquals(restored.portrait.id, "custom");
        assertEquals(restored.portraitPng, new byte[]{1, 2, 3});
        restored.portraitPng[1] = 88;
        assertEquals(CharacterIdentity.fromSaveData(saved).portraitPng, new byte[]{1, 2, 3});
    }
    @Test public void absentIdentityRetainsLegacySaveSelection() {
        assertNull(CharacterIdentity.fromSaveData(null));
    }
    @Test public void builtInPortraitRetainsStableResourceAndRegionRatherThanMenuIndex() {
        CharacterIdentity identity = new CharacterIdentity(); identity.portrait = new Portrait();
        identity.portrait.id = "wayfarer"; identity.portrait.image = "portraits/paintings.png";
        identity.portrait.x = 627; identity.portrait.width = 627; identity.portrait.height = 627;
        CharacterIdentity restored = CharacterIdentity.fromSaveData(identity.toSaveData());
        assertNotNull(restored);
        assertEquals(restored.portrait.id, "wayfarer");
        assertEquals(restored.portrait.image, "portraits/paintings.png");
        assertEquals(restored.portrait.x, 627);
        assertEquals(restored.portrait.width, 627);
        assertEquals(restored.portraitPng.length, 0);
    }
    @Test public void unsupportedIdentityVersionUsesLegacyFallback() {
        SaveFileData saved = new SaveFileData(); saved.store("version", 999);
        assertNull(CharacterIdentity.fromSaveData(saved));
    }
}
