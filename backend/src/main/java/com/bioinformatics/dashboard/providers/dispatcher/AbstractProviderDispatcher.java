package com.bioinformatics.dashboard.providers.dispatcher;

import com.bioinformatics.dashboard.interfaces.Provider;
import com.bioinformatics.dashboard.providers.ProviderContextHolder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract base for provider dispatchers.
 * Builds a registry of provider implementations and resolves the active provider at request time.
 *
 * @param <T> service interface type (extends Provider)
 */
@Slf4j
public abstract class AbstractProviderDispatcher<T extends Provider> implements Provider {
    private final Map<String, T> services;

    /**
     * Initialize dispatcher with list of provider implementations.
     * Builds a map of providerName -> implementation and logs registry size.
     *
     * @param services list of all implementations of the service interface
     */
    protected AbstractProviderDispatcher(List<T> services) {
        this.services = services.stream()
                .filter(e -> !e.getProviderName().isBlank())
                .collect(Collectors.toMap(Provider::getProviderName, e -> e));

        log.info("{} Registry: found {} providers", this.getClass().getSimpleName(), this.services.size());
    }

    /**
     * Dispatchers do not have their own provider name.
     *
     * @return empty string
     */
    @Override
    public String getProviderName() {
        return ""; // Dispatchers don't have a provider name themselves
    }

    /**
     * Resolve the active provider implementation based on ProviderContextHolder.
     * Throws RuntimeException if provider name not found in registry.
     *
     * @return concrete provider implementation
     * @throws RuntimeException if provider not found
     */
    protected T resolve() {
        var name = ProviderContextHolder.get();
        var provider = services.get(name);
        if (provider == null) {
            throw new RuntimeException("No provider found with name <%s>".formatted(name));
        }
        return provider;
    }
}
