package forge.adventure.character;

import forge.adventure.data.DifficultyData;
import forge.adventure.util.Config;
import forge.adventure.util.AdventureOverrides;
import forge.util.MyRandom;
import java.util.Random;
import forge.adventure.util.AdventureModes;
import forge.card.ColorSet;
import java.util.function.Supplier;

/** A render-thread-only preview scope, separate from campaign activation and live run state. */
public final class StarterDeckPreview {
    private static final ThreadLocal<Context> current = new ThreadLocal<>();
    private static final class Context {
        final Config resources;
        final DifficultyData difficulty;
        final AdventureModes mode;
        final ColorSet color;
        final Random random = new Random();
        AdventureOverrides overrides;
        Context(Config resources, DifficultyData difficulty, AdventureModes mode, ColorSet color) {
            this.resources = resources; this.difficulty = difficulty; this.mode = mode; this.color = color;
        }
    }
    private StarterDeckPreview() { }
    public static <T> T generate(Config resources, DifficultyData difficulty, AdventureModes mode, ColorSet color, Supplier<T> builder) {
        Context previous = current.get(), preview = new Context(resources, difficulty, mode, color);
        current.set(preview);
        try {
            forge.adventure.data.RewardData.invalidateCardPool();
            return MyRandom.withRandom(preview.random, () -> {
                preview.overrides = AdventureOverrides.forPreview(resources);
                return builder.get();
            });
        } finally {
            if (previous == null) current.remove(); else current.set(previous);
            forge.adventure.data.RewardData.invalidateCardPool();
        }
    }
    public static Config resources() { Context context = current.get(); return context == null ? null : context.resources; }
    public static Random randomOr(Random fallback) { Context context = current.get(); return context == null ? fallback : context.random; }
    public static AdventureOverrides overridesOr(AdventureOverrides fallback) { Context context = current.get(); return context == null || context.overrides == null ? fallback : context.overrides; }
    public static float rewardFactorOr(float fallback) { Context context = current.get(); return context == null ? fallback : context.difficulty.rewardMaxFactor; }
    public static boolean commanderOr(boolean fallback) { Context context = current.get(); return context == null ? fallback : context.mode.isCommanderLike(); }
    public static ColorSet colorOr(ColorSet fallback) { Context context = current.get(); return context == null ? fallback : context.color; }
    public static int bonusCardsOr(int fallback) { return current.get() == null ? fallback : 0; }
}
