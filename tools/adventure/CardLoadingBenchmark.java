import forge.CardStorageReader;
import forge.StaticData;
import forge.card.CardType;
import forge.util.FileSection;
import forge.util.FileUtil;
import forge.util.Localizer;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;

/** Headless, memory-capped benchmark. Uses no player profile and starts no game. */
public final class CardLoadingBenchmark {
    private static long elapsed(long start) { return (System.nanoTime() - start) / 1_000_000; }
    private static int fieldSize(Object owner, String name) throws Exception {
        var field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        Object value = field.get(owner);
        if (value instanceof Map<?, ?> map) return map.size();
        if (value instanceof com.google.common.collect.Multimap<?, ?> map) return map.size();
        throw new IllegalStateException(name);
    }
    public static void main(String[] args) throws Exception {
        Path resources = Path.of(args[0]).toAbsolutePath();
        boolean lazy = args[1].equals("lazy");
        long started = System.nanoTime();
        Localizer.getInstance().initialize("en-US", resources.resolve("languages").toString());
        forge.util.Lang.createInstance("en-US");
        var types = FileSection.parseSections(FileUtil.readFile(resources.resolve("lists/TypeLists.txt").toString()));
        for (var entry : types.entrySet()) CardType.Helper.parseTypes(entry.getKey(), entry.getValue());
        CardType.Constant.LOADED.set();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", args[1]);
        result.put("java", System.getProperty("java.version"));
        long stage = System.nanoTime();
        var reader = args.length > 3
                ? CardStorageReader.class.getConstructor(String.class, CardStorageReader.ProgressObserver.class, boolean.class, Path.class)
                    .newInstance(resources.resolve("cardsfolder").toString(), null, lazy, Path.of(args[3]))
                : new CardStorageReader(resources.resolve("cardsfolder").toString(), null, lazy);
        result.put("card_reader_ms", elapsed(stage));
        if (args.length > 3) {
            result.put("index_cache", CardStorageReader.class.getMethod("getNameIndexCacheStatus").invoke(reader));
            result.put("index_scripts_parsed", CardStorageReader.class.getMethod("getNameIndexParseCount").invoke(reader));
        }
        var tokens = new CardStorageReader(resources.resolve("tokenscripts").toString(), null, false);
        stage = System.nanoTime();
        var data = new StaticData(reader, tokens, null, null,
                resources.resolve("editions").toString(), resources.resolve("no-custom-editions").toString(),
                resources.resolve("blockdata").toString(), resources.resolve("setlookup").toString(),
                "Latest Art All Editions", false, false, false, false);
        result.put("database_ms", elapsed(stage));
        result.put("rules_before", fieldSize(data.getCommonCards(), "rulesByPrimaryName"));
        result.put("printings_before", fieldSize(data.getCommonCards(), "allCardsByRules"));
        stage = System.nanoTime();
        for (String card : List.of("Plains", "Mountain", "Elite Vanguard", "Pacifism", "Lightning Strike",
                "Goblin Piker", "Serra Angel", "Ghoulcaller's Bell", "Traveler's Amulet")) {
            if (data.getCommonCards().getCard(card) == null) throw new AssertionError("Missing " + card);
        }
        result.put("named_lookup_ms", elapsed(stage));
        result.put("rules_after", fieldSize(data.getCommonCards(), "rulesByPrimaryName"));
        result.put("printings_after", fieldSize(data.getCommonCards(), "allCardsByRules"));
        if (Boolean.getBoolean("forge.benchmark.complete")) {
            stage = System.nanoTime();
            result.put("complete_printings", data.getCommonCards().getAllCards().size());
            result.put("complete_rules", data.getCommonCards().getUniqueCards().size());
            result.put("complete_catalog_ms", elapsed(stage));
        }
        result.put("total_ms", elapsed(started));
        result.put("used_heap_mb", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() / (1024 * 1024));
        result.put("peak_heap_pool_mb", ManagementFactory.getMemoryPoolMXBeans().stream()
                .filter(pool -> pool.getType() == java.lang.management.MemoryType.HEAP)
                .mapToLong(pool -> pool.getPeakUsage().getUsed()).sum() / (1024 * 1024));
        String json = new Gson().toJson(result);
        Files.writeString(Path.of(args[2]), json + "\n");
        System.out.println("CARD_LOADING_BENCHMARK " + json);
        System.exit(0);
    }
}
