package com.bioinformatics.dashboard.providers.uniprotkb;

import com.bioinformatics.dashboard.interfaces.Provider;

public abstract class AbstractUniprotKbProvider implements Provider {
    @Override
    public String getProviderName() {
        return "uniprotKb";
    }
}
