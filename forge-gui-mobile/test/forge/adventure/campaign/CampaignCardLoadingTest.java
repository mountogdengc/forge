package forge.adventure.campaign;

import com.badlogic.gdx.utils.Array;
import forge.CardStorageReader;
import forge.Forge;
import forge.GuiMobile;
import forge.StaticData;
import forge.adventure.data.RewardData;
import forge.adventure.data.SettingData;
import forge.adventure.util.CardUtil;
import forge.adventure.util.Config;
import forge.adventure.util.Reward;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.util.Localizer;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.testng.Assert.*;

/** Real three-script database: never initializes the full catalog or a graphics application. */
@Test(singleThreaded = true)
public class CampaignCardLoadingTest {
    @Test
    public void unassignedNamedRewardAndSavedPrintingResolveWithoutTheCatalog() throws Exception {
        try (Fixture fixture = new Fixture(true)) {
            fixture.activate("ALL");
            assertNotNull(fixture.data.getOrLoadCommonCard("Unassigned Card", forge.card.CardEdition.UNKNOWN_CODE, 1, false));
            RewardData reward = new RewardData();
            reward.type = "card";
            reward.cardName = "Unassigned Card";
            reward.count = 1;
            assertEquals(reward.generate(false, true).first().getCard().getName(), "Unassigned Card");
            assertFalse(fixture.data.getCommonCards().contains("Shared Card"));
        }
    }
    @Test
    public void namedAndCurrencyRewardsDoNotLoadTheRandomRewardCatalog() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.activate("AAA");
            RewardData fixed = new RewardData();
            fixed.type = "card";
            fixed.cardName = "Alpha Card";
            fixed.count = 2;
            Array<Reward> cards = fixed.generate(false, true);
            assertEquals(cards.size, 2);
            assertEquals(cards.first().getCard().getName(), "Alpha Card");
            assertFalse(fixture.data.getCommonCards().contains("Shared Card"), "Named rewards must not initialize a random pool.");
            assertFalse(fixture.data.getCommonCards().contains("Beta Card"));

            RewardData union = new RewardData();
            union.type = "Union";
            union.count = 1;
            union.cardUnion = new RewardData[]{fixed};
            assertEquals(union.generate(false, true).first().getCard().getName(), "Alpha Card");
            assertFalse(fixture.data.getCommonCards().contains("Shared Card"));

            RewardData gold = new RewardData();
            gold.type = "gold";
            gold.count = 17;
            assertEquals(gold.generate(false, true).first().getCount(), 17);
            assertFalse(fixture.data.getCommonCards().contains("Shared Card"));
            assertFalse(fixture.data.getCommonCards().contains("Beta Card"));
        }
    }

    @Test
    public void allowedPoolsAreCompleteAndCampaignSwitchDoesNotReuseThePreviousPool() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.activate("AAA");
            assertEquals(names(CardUtil.getFullCardPool(false)), Set.of("Alpha Card", "Shared Card"));
            assertFalse(fixture.data.getCommonCards().contains("Beta Card"), "An explicit set pool must not need unrelated scripts.");
            assertTrue(CardUtil.getFullCardPool(true).stream().allMatch(card -> card.getEdition().equals("AAA")));
            assertEquals(names(RewardData.getAllCards()), Set.of("Alpha Card", "Shared Card"));

            // Do not explicitly invalidate: Config identity must protect callers that only activate a preview.
            fixture.activate("BBB");
            assertEquals(names(RewardData.getAllCards()), Set.of("Beta Card", "Shared Card"));
            assertEquals(names(CardUtil.getFullCardPool(false)), Set.of("Beta Card", "Shared Card"));
            assertTrue(CardUtil.getFullCardPool(true).stream().allMatch(card -> card.getEdition().equals("BBB")));
            fixture.activate("AAA");
            assertEquals(names(RewardData.getAllCards()), Set.of("Alpha Card", "Shared Card"));
        }
    }

    @Test
    public void campaignWithoutAnAllowedSetListStillGetsTheCompleteCatalog() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.activate("ALL");
            assertEquals(names(CardUtil.getFullCardPool(false)), Set.of("Alpha Card", "Beta Card", "Shared Card"));
            assertEquals(CardUtil.getFullCardPool(true).size(), 4);
        }
    }

    @Test
    public void explicitEditionRewardsStayLazyForUniqueAndVariantModes() throws Exception {
        for (boolean variants : new boolean[]{false, true}) {
            try (Fixture fixture = new Fixture()) {
                fixture.activate("ALL");
                Config.instance().getSettingData().useAllCardVariants = variants;
                RewardData fixed = new RewardData();
                fixed.type = "card";
                fixed.cardName = "Shared Card";
                fixed.editions = new String[]{"AAA"};
                fixed.count = 2;
                Array<Reward> cards = fixed.generate(false, true);
                assertEquals(cards.size, 2);
                for (Reward card : cards) assertEquals(card.getCard().getEdition(), "AAA");

                RewardData union = new RewardData();
                union.type = "Union";
                union.count = 2;
                union.cardUnion = new RewardData[]{fixed};
                Array<Reward> unionCards = union.generate(false, true);
                assertEquals(unionCards.size, 2);
                for (Reward card : unionCards) assertEquals(card.getCard().getEdition(), "AAA");

                fixed.cardName = "Shared Card|BBB";
                for (Reward card : fixed.generate(false, true)) assertEquals(card.getCard().getEdition(), "BBB",
                        "An embedded edition takes precedence over the separate edition filter.");
                assertFalse(fixture.data.getCommonCards().contains("Alpha Card"));
                assertFalse(fixture.data.getCommonCards().contains("Beta Card"),
                        "Explicit named rewards must not materialize unrelated scripts.");
            }
        }
    }

    private static Set<String> names(Iterable<PaperCard> cards) {
        Set<String> names = new java.util.HashSet<>();
        for (PaperCard card : cards) names.add(card.getName());
        return names;
    }

    private static final class Fixture implements AutoCloseable {
        final Path root = Files.createTempDirectory("campaign-card-loading-");
        final Field configField = field(Config.class, "currentConfig");
        final Object previousConfig = configField.get(null);
        final Field dataField = field(StaticData.class, "lastInstance");
        final Object previousData = dataField.get(null);
        final IGuiBase previousGui = GuiBase.getInterface();
        final Field localizerField = field(Localizer.class, "instance");
        final Object previousLocalizer = localizerField.get(null);
        final Field forgeLocalizerField = field(Forge.class, "localizer");
        final Object previousForgeLocalizer = forgeLocalizerField.get(null);
        final StaticData data;
        final MemoizedValue database;

        Fixture() throws Exception {
            this(false);
        }

        Fixture(boolean unknown) throws Exception {
            Path resources = Path.of("forge-gui/res");
            if (!Files.isDirectory(resources)) resources = Path.of("../forge-gui/res");
            resources = resources.toAbsolutePath().normalize();
            if (GuiBase.getInterface() == null) GuiBase.setInterface(new GuiMobile(resources.getParent() + "/"));
            localizerField.set(null, null);
            Localizer.getInstance().initialize("en-US", resources.resolve("languages").toString());
            forgeLocalizerField.set(null, Localizer.getInstance());
            Path scripts = Files.createDirectories(root.resolve("cards"));
            if (unknown) Files.writeString(scripts.resolve("unassigned.txt"),
                    "Name:Unassigned Card\nManaCost:W\nTypes:Sorcery\nOracle:Fixture.\n");
            for (String name : List.of("Alpha Card", "Beta Card", "Shared Card")) {
                Files.writeString(scripts.resolve(name.replace(' ', '_') + ".txt"),
                        "Name:" + name + "\nManaCost:W\nTypes:Sorcery\nOracle:Fixture card.\n");
            }
            Path editions = Files.createDirectories(root.resolve("editions"));
            edition(editions, "AAA", "Alpha Card", "2000-01-01");
            edition(editions, "BBB", "Beta Card", "2001-01-01");
            // CardUtil caches standard format predicates at class initialization. Keep their real set
            // metadata available so this fixture cannot leave invalid format predicates in the JVM.
            // This reads edition descriptions only, never the real card scripts.
            data = new StaticData(new CardStorageReader(scripts.toString(), null, true), null,
                    ForgeConstants.EDITIONS_DIR, editions.toString(),
                    root.toString(), "Latest Art All Editions", unknown, false);
            database = new MemoizedValue("magicDb", data);
            RewardData.invalidateCardPool();
        }

        void activate(String edition) throws Exception {
            Path chapter = Files.createDirectories(root.resolve(edition));
            Files.createDirectories(chapter.resolve("world"));
            Files.writeString(chapter.resolve("world/world.json"), "{width:8,height:8,tileSize:16}");
            Files.writeString(chapter.resolve("world/quests.json"), "[]");
            Files.writeString(chapter.resolve("config.json"), "{screenWidth:480,screenHeight:270,colorIds:[W],"
                    + "colorIdNames:[White],starterEditions:[],starterEditionNames:[],difficulties:[{name:Normal}],"
                    + "restrictedCards:[],restrictedEditions:[],allowedEditions:[" + (edition.equals("ALL") ? "" : edition) + "]}");
            CampaignDefinition campaign = new CampaignDefinition(edition, "1", edition, "", edition,
                    chapter, root, new String[0], null, false, false, false, false, false);
            Config.activateCampaign(Config.forCampaign(campaign, new SettingData()));
        }

        private void edition(Path editions, String code, String unique, String date) throws Exception {
            Files.writeString(editions.resolve(code + ".txt"), "[metadata]\nCode=" + code + "\nName=" + code
                    + "\nDate=" + date + "\nType=Core\n[cards]\n1 C " + unique + "\n2 C Shared Card\n");
        }

        @Override public void close() throws Exception {
            RewardData.invalidateCardPool();
            configField.set(null, previousConfig);
            database.close();
            dataField.set(null, previousData);
            localizerField.set(null, previousLocalizer);
            forgeLocalizerField.set(null, previousForgeLocalizer);
            GuiBase.setInterface(previousGui);
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    /** FModel has no injectable database boundary; scope its memoized value to this tiny fixture. */
    private static final class MemoizedValue implements AutoCloseable {
        final Object supplier;
        final Field value;
        final Object oldValue;
        final Field completion;
        final Object oldCompletion;

        MemoizedValue(String name, Object replacement) throws Exception {
            supplier = field(FModel.class, name).get(null);
            value = field(supplier.getClass(), "value");
            oldValue = value.get(supplier);
            Field state;
            Object completed;
            try {
                state = field(supplier.getClass(), "initialized");
                completed = true;
            } catch (NoSuchFieldException e) {
                state = field(supplier.getClass(), "delegate");
                completed = field(supplier.getClass(), "SUCCESSFULLY_COMPUTED").get(null);
            }
            completion = state;
            oldCompletion = completion.get(supplier);
            value.set(supplier, replacement);
            completion.set(supplier, completed);
        }

        @Override public void close() throws Exception {
            value.set(supplier, oldValue);
            completion.set(supplier, oldCompletion);
        }
    }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
