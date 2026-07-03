package com.bioinformatics.dashboard.providers;

/**
 * Thread-scoped context holder for the current data provider.
 * Allows runtime selection of provider implementation per HTTP request.
 */
public class ProviderContextHolder {

    private static final ThreadLocal<String> CURRENT_PROVIDER = new ThreadLocal<>();

    /**
     * Set the provider for the current thread.
     *
     * @param provider provider name (e.g., "postgres", "mongo")
     */
    public static void set(String provider) {
        CURRENT_PROVIDER.set(provider);
    }

    /**
     * Get the provider for the current thread.
     * @return provider name or null if not set
     */
    public static String get() {
        return CURRENT_PROVIDER.get();
    }

    /**
     * Clear the provider context (typically done after request processing).
     */
    public static void clear() {
        CURRENT_PROVIDER.remove();
    }

}
