package forge.adventure.character;

import forge.adventure.data.CharacterCreationData;
import forge.adventure.data.CharacterCreationData.*;
import forge.adventure.data.HeroData;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

public class CharacterCreationCatalogTest {
    private static HeroData hero() {
        HeroData h = new HeroData(); h.name = "Human";
        h.male = "sprites/human-m.atlas"; h.female = "sprites/human-f.atlas";
        return h;
    }
    private static Portrait portrait(String id, String race, String gender) {
        Portrait p = new Portrait(); p.id = id; p.races = new String[]{race}; p.genders = new String[]{gender};
        return p;
    }
    private static CharacterCreationCatalog catalog() {
        return new CharacterCreationCatalog(null, new HeroData[]{hero()}, List.of(
                portrait("human-f", "human", "female"), portrait("human-m", "human", "male"),
                portrait("elf-f", "elf", "female")));
    }
    @Test public void inheritedOptionsRemainAvailableWithIndependentGenderAndAppearance() {
        var catalog = catalog();
        assertEquals(catalog.genders.size(), 4);
        assertEquals(catalog.appearances.size(), 2);
        assertEquals(catalog.matchingPortraits("human", "male", false, false).size(), 3);
        assertFalse(catalog.settings.matchPortraitGender);
        assertFalse(catalog.settings.matchPortraitRace);
    }
    @Test public void optionalRaceAndGenderFiltersIntersect() {
        var values = catalog().matchingPortraits("human", "female", true, true);
        assertEquals(values.stream().map(p -> p.id).toList(), List.of("human-f"));
    }
    @Test public void unspecifiedGenderDoesNotFilterPortraitsAndMissingMatchesAreEmpty() {
        assertEquals(catalog().matchingPortraits("human", "unspecified", true, true).size(), 2);
        assertTrue(catalog().matchingPortraits("dwarf", "female", true, true).isEmpty());
    }
    @Test public void campaignSettingsDoNotLeakIntoInheritedCampaigns() {
        var data = new CharacterCreationData(); data.allowPortraitImport = false;
        var curated = new CharacterCreationCatalog(data, new HeroData[]{hero()}, List.of());
        assertFalse(curated.settings.allowPortraitImport);
        assertTrue(catalog().settings.allowPortraitImport);
    }
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void duplicatePersistentIdsAreRejected() {
        var data = new CharacterCreationData();
        data.portraits = new Portrait[]{portrait("same", "human", "male"), portrait("same", "elf", "male")};
        new CharacterCreationCatalog(data, new HeroData[]{hero()}, List.of());
    }
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void explicitEmptyGenderCatalogDoesNotCrashCycling() {
        var data = new CharacterCreationData(); data.genders = new Gender[0];
        new CharacterCreationCatalog(data, new HeroData[]{hero()}, List.of());
    }
}
