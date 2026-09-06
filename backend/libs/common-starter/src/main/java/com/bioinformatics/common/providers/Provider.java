package com.bioinformatics.common.providers;

/**
 * Marker interface for pluggable data providers.
 * All provider implementations must identify themselves with a unique name.
 */
public interface Provider {

    /**
     * Returns the unique identifier of this provider.
     *
     * @return provider name (e.g., "postgres", "mongo", "rdf")
     */
    String getProviderName();
}
