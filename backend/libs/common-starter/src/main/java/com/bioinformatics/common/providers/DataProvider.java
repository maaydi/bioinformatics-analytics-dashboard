package com.bioinformatics.common.providers;

import lombok.Getter;

import static com.bioinformatics.shared.models.security.Constants.API_DATA_PROVIDER;
import static com.bioinformatics.shared.models.security.Constants.POSTGRES_DATA_PROVIDER;

/**
 * Shared provider identifiers used by the platform to route data access toward a concrete backend.
 */
@Getter
public enum DataProvider {
    API(API_DATA_PROVIDER),
    POSTGRES(POSTGRES_DATA_PROVIDER);

    private final String key;

    DataProvider(String key) {
        this.key = key;
    }

    /**
     * Checks whether the given provider key matches the API-backed provider.
     *
     * @param provider provider key to evaluate
     * @return {@code true} when the provider is the API provider
     */
    public static boolean isApi(String provider) {
        return provider.equalsIgnoreCase(API.getKey());
    }
}
