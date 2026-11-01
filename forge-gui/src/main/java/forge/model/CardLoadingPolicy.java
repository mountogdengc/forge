package forge.model;

/** Keep native mobile and other desktop startup defaults, while deferring Adventure's unused rules. */
public final class CardLoadingPolicy {
    private CardLoadingPolicy() { }

    public static boolean useLazyScripts(boolean nativeMobile, boolean libgdx, String selectorMode,
                                         boolean userPreference, boolean forceEager) {
        return !nativeMobile && !forceEager && (userPreference || (libgdx && "Adventure".equals(selectorMode)));
    }
}
