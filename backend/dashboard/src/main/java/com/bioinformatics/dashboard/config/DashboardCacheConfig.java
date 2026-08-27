package com.bioinformatics.dashboard.config;

import com.bioinformatics.common.config.cache.CacheRegistryProvider;
import com.bioinformatics.common.models.filter.SavedFilterDto;
import com.bioinformatics.dashboard.model.gene.PagedResponse;
import com.bioinformatics.dashboard.model.gene.ProteinSummaryDto;
import com.bioinformatics.shared.models.cache.TypedCacheSpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DashboardCacheConfig {

    @Bean
    public CacheRegistryProvider analyticsCacheProvider() {
        return () -> List.of(
                new TypedCacheSpec(PagedResponse.class, SavedFilterDto.class, "savedFilters"),
                new TypedCacheSpec(PagedResponse.class, ProteinSummaryDto.class, "geneList", "geneSearch", "geneList-kb", "geneSearch-kb"),
                new TypedCacheSpec(List.class, String.class, "geneKeywords"));
    }
}