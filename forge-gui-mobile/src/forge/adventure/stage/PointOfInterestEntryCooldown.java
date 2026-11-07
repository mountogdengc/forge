package forge.adventure.stage;

/** Briefly suppresses world-map entry triggers after leaving an interior. */
final class PointOfInterestEntryCooldown {
    private float remaining;

    void start() {
        remaining = 1.5f;
    }

    void update(float delta) {
        remaining = Math.max(0f, remaining - delta);
    }

    boolean isActive() {
        return remaining > 0f;
    }
}
