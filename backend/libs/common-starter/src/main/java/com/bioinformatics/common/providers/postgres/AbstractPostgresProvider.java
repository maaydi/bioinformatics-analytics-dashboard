package com.bioinformatics.common.providers.postgres;

import com.bioinformatics.common.providers.Provider;

public abstract class AbstractPostgresProvider implements Provider {
    @Override
    public String getProviderName() {
        return "postgres";
    }
}
