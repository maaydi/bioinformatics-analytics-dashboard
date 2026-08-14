package com.bioinformatics.dashboard.providers.uniprotkb;

import com.bioinformatics.dashboard.interfaces.Provider;

/**
 * Abstract base class for UniProt KB provider implementations.
 *
 * <p>Establishes the provider identity as "uniprotKb" across all concrete
 * implementations of UniProt KB data access. This class centralizes the provider
 * name to ensure consistency and simplify maintenance.</p>
 */
public abstract class AbstractUniprotKbProvider implements Provider {
    @Override
    public String getProviderName() {
        return "uniprotKb";
    }
}
