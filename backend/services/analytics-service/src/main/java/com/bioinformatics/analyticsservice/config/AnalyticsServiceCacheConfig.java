package com.bioinformatics.analyticsservice.config;

import com.bioinformatics.analyticsservice.models.*;
import com.bioinformatics.common.config.cache.CacheRegistryProvider;
import com.bioinformatics.shared.models.cache.TypedCacheSpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AnalyticsServiceCacheConfig {

    @Bean
    public CacheRegistryProvider analyticsCacheProvider() {
        return () -> List.of(
                new TypedCacheSpec(List.class, OrganismCountDto.class, "byOrganism", "filtered-byOrganism"),
                new TypedCacheSpec(List.class, LengthHistogramBucketDto.class, "lengthHistogram", "filtered-lengthHistogram"),
                new TypedCacheSpec(List.class, ReviewedRatioDto.class, "reviewedRatio", "filtered-reviewedRatio"),
                new TypedCacheSpec(List.class, EvidenceDistributionDto.class, "evidenceLevels", "filtered-evidenceLevels"),
                new TypedCacheSpec(List.class, KeywordFrequencyDto.class, "keywordFrequency", "filtered-keywordFrequency"),
                new TypedCacheSpec(List.class, ProteinLengthWeightCount.class, "filtered-proteinLengthWeightCount")
        );
    }
}