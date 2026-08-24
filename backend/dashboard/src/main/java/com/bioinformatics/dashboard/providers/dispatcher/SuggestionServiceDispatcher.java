package com.bioinformatics.dashboard.providers.dispatcher;

import com.bioinformatics.common.providers.AbstractProviderDispatcher;
import com.bioinformatics.common.providers.ProviderContextHolder;
import com.bioinformatics.dashboard.interfaces.suggest.SuggestionService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Dispatcher for GeneService implementations.
 * Routes all gene operations to the active provider based on ProviderContextHolder.
 * Marked as @Primary so controllers inject this dispatcher instead of concrete implementations.
 */
@Service
@Primary
public class SuggestionServiceDispatcher extends AbstractProviderDispatcher<SuggestionService> implements SuggestionService {


    public SuggestionServiceDispatcher(List<SuggestionService> services) {
        super(services);
    }

    @Override
    public String field() {
        return "";
    }

    @Override
    public List<String> suggest(String query) {
        return resolve().suggest(query);
    }

    @Override
    public List<String> suggest(String field, String query) {
        var activeProvider = ProviderContextHolder.get();
        var compositeKey = activeProvider + "-" + field;

        return resolveByKey(compositeKey).suggest(query);
    }

    @Override
    public String getProviderName() {
        return "";
    }

    @Override
    protected String getServiceName(SuggestionService service) {
        return service.getProviderName() + "-" + service.field();
    }

    @Override
    protected boolean includeService(SuggestionService service) {
        return !service.getProviderName().isBlank() && !service.field().isBlank();
    }
}
