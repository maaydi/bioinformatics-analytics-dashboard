package com.bioinformatics.analyticsservice.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsServiceCacheConfigTest {

    @Test
    void registersExpectedTypedCacheSpecs() {
        var config = new AnalyticsServiceCacheConfig();

        var specs = config.analyticsCacheProvider().getCacheSpecs();

        assertThat(specs).hasSize(6);
        assertThat(specs)
                .extracting(spec -> spec.cacheNames()[0])
                .containsExactly("byOrganism", "lengthHistogram", "reviewedRatio", "evidenceLevels", "keywordFrequency", "filtered-proteinLengthWeightCount");
        assertThat(specs)
                .allSatisfy(spec -> assertThat(spec.parameterizedType()).isEqualTo(java.util.List.class));
    }
}

