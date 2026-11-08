package forge.card;

import forge.item.PaperCard;
import forge.util.ItemPool;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

public class PrintSheetSupportTest {
    private PaperCard supported(String name) {
        return new PaperCard(CardRules.fromScript(List.of("Name:" + name, "ManaCost:W",
                "Types:Sorcery", "Oracle:Fixture.")), "TST", CardRarity.Common);
    }

    private PaperCard unsupported() {
        return new PaperCard(CardRules.getUnsupportedCardNamed("Missing Script"), "TST", CardRarity.Rare);
    }

    @Test
    public void everyInsertionPathRejectsUnsupportedScripts() {
        PaperCard valid = supported("Supported Spell"), missing = unsupported();
        PrintSheet sheet = new PrintSheet("fixture");
        sheet.add(missing, 9);
        sheet.addAll(Arrays.asList(valid, missing, null), 3);
        sheet.add(null);
        assertEquals(sheet.toNameLookup(), Map.of("Supported Spell", 3));
        assertEquals(sheet.random(2, false), List.of(valid, valid));
    }

    @Test
    public void constructorFiltersAndCopiesWithoutChangingWeightsOrItsInput() {
        PaperCard valid = supported("Supported Spell"), missing = unsupported();
        ItemPool<PaperCard> input = new ItemPool<>(PaperCard.class);
        input.add(valid, 4);
        input.add(missing, 8);
        PrintSheet sheet = new PrintSheet("fixture", input);
        assertEquals(sheet.toNameLookup(), Map.of("Supported Spell", 4));
        assertEquals(input.count(missing), 8, "Filtering cannot mutate a caller-owned pool");
        sheet.add(valid, 2);
        assertEquals(input.count(valid), 4, "A sheet must own its weighted entries");
        assertEquals(sheet.toNameLookup(), Map.of("Supported Spell", 6));
    }

    @Test
    public void anEntirelyUnsupportedSheetReturnsNoPlaceholderCards() {
        PrintSheet sheet = new PrintSheet("unsupported");
        sheet.add(unsupported());
        assertTrue(sheet.isEmpty());
        assertTrue(sheet.random(1, true).isEmpty());
    }
}
