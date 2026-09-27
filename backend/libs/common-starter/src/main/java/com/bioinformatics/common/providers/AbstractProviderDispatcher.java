package com.bioinformatics.common.providers;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract base class for provider dispatchers.
 *
 * <p>Dispatcher implementations resolve the active concrete provider at runtime based on the
 * current {@link ProviderContextHolder}. This allows a single service interface to support
 * multiple backend implementations such as PostgreSQL, MongoDB, or RDF providers.
 *
 * <p>Design note: the dispatcher itself is not a provider; it delegates execution to the active one.
 *
 * @param <T> service interface type extending {@link Provider}
 */
@Slf4j
public abstract class AbstractProviderDispatcher<T extends Provider> implements Provider {
    private final Map<String, T> services;

    /**
     * Initializes the dispatcher registry for all provider implementations.
     *
     * @param services list of all implementations of the service interface
     */
    protected AbstractProviderDispatcher(List<T> services) {
        this.services = services.stream()
                .filter(this::includeService)
                .collect(Collectors.toMap(s -> getServiceName(s).toLowerCase(), e -> e));

        log.info("[COMMON_PROVIDER] {} registry initialized - providerCount={}",
                this.getClass().getSimpleName(), this.services.size());
    }

    /**
     * Dispatchers do not have their own provider identity.
     *
     * @return empty string
     */
    @Override
    public String getProviderName() {
        return "";
    }

    /**
     * Resolves the active provider implementation using the current thread-bound context.
     *
     * @return concrete provider implementation selected for this request
     */
    protected T resolve() {
        var key = ProviderContextHolder.get();
        log.debug("[COMMON_PROVIDER] Resolving active provider - key={}", key);
        return resolveByKey(key);
    }

    /**
     * Resolves a provider implementation by key.
     *
     * @param key provider identifier, e.g. postgres, mongo, rdf
     * @return implementation mapped to the key
     * @throws IllegalArgumentException when the key is null/blank
     * @throws RuntimeException         when no provider matches the key
     */
    protected T resolveByKey(String key) {
        if (key == null || key.isBlank()) {
            log.warn("[COMMON_PROVIDER] Provider resolution failed - key was null or blank");
            throw new IllegalArgumentException("Lookup key cannot be null or blank");
        }

        T provider = services.get(key.toLowerCase());
        if (provider == null) {
            log.error("[COMMON_PROVIDER] No provider found for key='{}' - registryKeys={}", key, services.keySet());
            throw new RuntimeException("No provider found with key <%s>".formatted(key));
        }

        log.debug("[COMMON_PROVIDER] Provider resolved - key='{}', provider={}", key, provider.getClass().getSimpleName());
        return provider;
    }

    /**
     * Returns the provider name for a registered implementation.
     *
     * @param service provider implementation
     * @return provider name
     */
    protected String getServiceName(T service) {
        return service.getProviderName();
    }

    /**
     * Whether to include a service in the registry.
     *
     * @param service provider implementation
     * @return true when the provider has a usable name
     */
    protected boolean includeService(T service) {
        return !service.getProviderName().isBlank();
    }
}
