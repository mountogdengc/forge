package forge;

import forge.card.CardDb;
import forge.util.Lang;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.*;

/** Tiny real catalogs exercise deferred loading without depending on the installed card archive. */
public class DeferredCardCatalogTest {
    @Test
    public void namedUnassignedCardsRemainAvailableBeforeFullEnumeration() throws Exception {
        StaticData data = fixture(true, true);
        assertNotNull(data.getCommonCards().getCard("Unassigned Spell"), "named UNKNOWN printing must be created on demand");
        assertEquals(data.getCommonCards().getCard("Unassigned Spell").getEdition(), forge.card.CardEdition.UNKNOWN_CODE);
        assertFalse(data.getCommonCards().contains("Outside Spell"));
    }
    @DataProvider(name = "wholeCatalogApis")
    public Object[][] wholeCatalogApis() {
        return new Object[][]{{"getAllCards", 4L}, {"getUniqueCards", 3L}, {"getAllFaces", 3L},
                {"streamAllCards", 4L}, {"streamUniqueCards", 3L}, {"streamAllFaces", 3L}};
    }

    @Test(dataProvider = "wholeCatalogApis")
    public void eachWholeCatalogApiCompletesAnUntouchedDatabase(String method, long expected) throws Exception {
        CardDb db = fixture(true).getCommonCards();
        Object result = CardDb.class.getMethod(method).invoke(db);
        long count = result instanceof java.util.stream.Stream<?> stream ? stream.count() : ((java.util.Collection<?>) result).size();
        assertEquals(count, expected);
    }

    @Test
    public void fullEnumerationCompletesPreviouslyRequestedPrinting() throws Exception {
        StaticData data = fixture(true);
        CardDb db = data.getCommonCards();
        data.attemptToLoadCard("Shared Spell", "AAA");
        assertEquals(db.getCard("Shared Spell").getEdition(), "AAA");
        assertEquals(db.getAllCards("Shared Spell").size(), 1);
        assertEquals(db.getAllCards().size(), 4, "whole catalog must include all printings and cards");
        assertEquals(db.getAllCards("Shared Spell").size(), 2, "explicit first printing must not truncate the full catalog");
        assertEquals(db.getUniqueCards().size(), 3);
        assertEquals(db.streamAllCards().count(), 4L);
        assertEquals(db.streamUniqueCards().count(), 3L);
        assertEquals(db.getAllFaces().size(), 3);
        assertEquals(db.streamAllFaces().count(), 3L);
        assertEquals(data.getCommonCards().getAllCards().size(), fixture(false).getCommonCards().getAllCards().size());
    }

    @Test
    public void selectedEditionsPrepareTheirEntirePoolAndOutsideNamesRemainAvailable() throws Exception {
        StaticData data = fixture(true);
        CardDb db = data.getCommonCards();
        assertEquals(loaded(db, false).size(), 0);
        java.util.Collection<?> before = loaded(db, true);
        StaticData.class.getMethod("preloadEditionCards", java.util.Collection.class).invoke(data, List.of("AAA"));
        assertEquals(loaded(db, true).size(), 2);
        assertEquals(before.size(), 0, "loaded snapshots must not change under their callers");
        assertFalse(db.contains("Outside Spell"), "selected edition preparation loaded unrelated rules");
        data.attemptToLoadCard("Outside Spell");
        assertNotNull(db.getCard("Outside Spell"), "outside-set effects and saved cards must remain resolvable");
        assertEquals(loaded(db, true).size(), 3);
        assertEquals(db.getAllCards().size(), 4);
    }

    @SuppressWarnings("unchecked")
    private static java.util.Collection<?> loaded(CardDb db, boolean unique) throws Exception {
        return (java.util.Collection<?>) CardDb.class.getMethod(unique ? "getLoadedUniqueCards" : "getLoadedAllCards").invoke(db);
    }

    private static StaticData fixture(boolean lazy) throws Exception {
        return fixture(lazy, false);
    }

    private static StaticData fixture(boolean lazy, boolean unassigned) throws Exception {
        Lang.createInstance("en-US");
        Path resources = Files.isDirectory(Path.of("forge-gui/res")) ? Path.of("forge-gui/res") : Path.of("../forge-gui/res");
        forge.util.Localizer.getInstance().initialize("en-US", resources.resolve("languages").toString());
        Path root = Files.createTempDirectory("deferred-catalog-");
        Path scripts = Files.createDirectories(root.resolve("cards"));
        Path editions = Files.createDirectories(root.resolve("editions"));
        Path custom = Files.createDirectories(root.resolve("custom"));
        if (unassigned) Files.writeString(scripts.resolve("unassigned.txt"), "Name:Unassigned Spell\nManaCost:1\nTypes:Sorcery\nOracle:Fixture.\n");
        for (String name : List.of("Shared Spell", "Local Spell", "Outside Spell")) {
            Files.writeString(scripts.resolve(name.replace(' ', '_') + ".txt"),
                    "Name:" + name + "\nManaCost:1\nTypes:Sorcery\nOracle:Fixture.\n");
        }
        Files.writeString(editions.resolve("first.txt"), "[metadata]\nCode=AAA\nDate=2020-01-01\nName=First Fixture\nType=Core\n[cards]\n1 C Shared Spell\n2 C Local Spell\n");
        Files.writeString(editions.resolve("second.txt"), "[metadata]\nCode=BBB\nDate=2021-01-01\nName=Second Fixture\nType=Core\n[cards]\n1 C Shared Spell\n2 C Outside Spell\n");
        return new StaticData(new CardStorageReader(scripts.toString(), null, lazy), null,
                editions.toString(), custom.toString(), root.toString(), "LATEST_ART_ALL_EDITIONS", true, true);
    }
}
