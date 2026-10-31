package forge.adventure.world;

/** Shared boundary for manual saves, automatic checkpoints and loading. */
public final class ExplorationSavePolicy {
    private ExplorationSavePolicy() { }

    public static boolean hasPendingAction(float remainingSeconds, boolean hasCallback) {
        return remainingSeconds > 0 || hasCallback;
    }

    public static String blockReason(boolean restoring, boolean transitioning, boolean dialogue, boolean battleOrReward) {
        if (restoring) return "Please wait for the adventure to finish loading.";
        if (transitioning) return "Please wait for the current action to finish before saving or loading.";
        if (dialogue) return "Finish this conversation before saving or loading.";
        if (battleOrReward) return "Finish this battle or reward selection before saving or loading.";
        return "";
    }
}
