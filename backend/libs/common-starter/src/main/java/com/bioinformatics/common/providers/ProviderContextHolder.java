package com.bioinformatics.common.providers;

/**
 * Thread-scoped context holder for the currently active data provider.
 *
 * <p>Each request can set a provider key such as {@code postgres}, {@code api}, or {@code rdf}; the
 * active provider is then resolved by the dispatcher for the current thread without leaking between
 * concurrent requests.
 */
public class ProviderContextHolder {

    private static final ThreadLocal<String> CURRENT_PROVIDER = new ThreadLocal<>();

    /**
     * Sets the provider for the current thread.
     *
     * @param provider provider name, for example {@code postgres} or {@code api}
     */
    public static void set(String provider) {
        CURRENT_PROVIDER.set(provider);
    }

    /**
     * Reads the current provider for this thread.
     *
     * @return provider name or {@code null} if not set
     */
    public static String get() {
        return CURRENT_PROVIDER.get();
    }

    /**
     * Clears provider context for the current thread.
     *
     * <p>This is typically called after request processing to avoid cross-request leakage in async work.
     */
    public static void clear() {
        CURRENT_PROVIDER.remove();
    }

}
