package com.bioinformatics.common.providers;


import lombok.Getter;

import static com.bioinformatics.shared.models.security.Constants.API_DATA_PROVIDER;
import static com.bioinformatics.shared.models.security.Constants.POSTGRES_DATA_PROVIDER;

@Getter
public enum DataProvider {
    API(API_DATA_PROVIDER),
    POSTGRES(POSTGRES_DATA_PROVIDER);
    private final String key;

    DataProvider(String key) {
        this.key = key;
    }

    public static boolean isApi(String provider) {
        return provider.equalsIgnoreCase(API.getKey());
    }
}
