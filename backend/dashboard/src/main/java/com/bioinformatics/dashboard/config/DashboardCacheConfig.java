package com.bioinformatics.dashboard.config;

import com.bioinformatics.common.config.cache.CacheRegistryProvider;
import com.bioinformatics.common.gene.dto.ProteinSummaryDto;
import com.bioinformatics.common.models.PagedResponse;
import com.bioinformatics.common.models.filter.SavedFilterDto;
import com.bioinformatics.shared.models.cache.TypedCacheSpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Registers the dashboard's cache namespaces and typed metadata used by Spring cache infrastructure.
 *
 * <p>The cache registry keeps the dashboard's read-heavy endpoints deterministic and prevents stale results across
 * saved-filter queries and gene list/search operations. Each cache entry is typed by payload class so that the shared
 * cache infrastructure can validate safe retrieval behavior.</p>
 */
@Configuration
public class DashboardCacheConfig {

    /**
     * Provides the dashboard-specific cache definitions consumed by the global cache registry.
     *
     * @return cache registry metadata for saved filters and gene result sets
     */
    @Bean
    public CacheRegistryProvider analyticsCacheProvider() {
        return () -> List.of(
                new TypedCacheSpec(PagedResponse.class, SavedFilterDto.class, "savedFilters"),
                new TypedCacheSpec(PagedResponse.class, ProteinSummaryDto.class, "geneList", "geneSearch", "geneList-kb", "geneSearch-kb"),
                new TypedCacheSpec(List.class, String.class, "geneKeywords"));
    }
}