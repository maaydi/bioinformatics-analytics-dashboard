package com.bioinformatics.dashboard.providers.dispatcher;

import com.bioinformatics.dashboard.interfaces.Provider;
import com.bioinformatics.dashboard.providers.ProviderContextHolder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractProviderDispatcher<T extends Provider> implements Provider {
    private final Map<String, T> services;

    protected AbstractProviderDispatcher(List<T> services) {
        this.services = services.stream()
                .filter(e -> !e.getProviderName().isBlank())
                .collect(Collectors.toMap(Provider::getProviderName, e -> e));

        log.info("{} Registry: found {} providers", this.getClass().getSimpleName(), this.services.size());
    }

    @Override
    public String getProviderName() {
        return ""; // Dispatchers don't have a provider name themselves
    }

    protected T resolve() {
        var name = ProviderContextHolder.get();
        var provider = services.get(name);
        if (provider == null) {
            throw new RuntimeException("No provider found with name <%s>".formatted(name));
        }
        return provider;
    }
}
