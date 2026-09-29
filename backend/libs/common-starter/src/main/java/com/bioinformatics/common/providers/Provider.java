package com.bioinformatics.common.providers;

/**
 * Contract for a pluggable data-provider implementation.
 *
 * <p>Provider implementations are resolved dynamically by {@link AbstractProviderDispatcher} based on
 * the active request context. Each implementation must expose a stable lookup key used for routing.
 */
public interface Provider {

    /**
     * Returns the unique identifier used to select this provider at runtime.
     *
     * @return provider key, such as {@code postgres}, {@code api}, or {@code rdf}
     */
    String getProviderName();
}
