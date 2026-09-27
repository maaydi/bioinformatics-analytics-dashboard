package com.bioinformatics.analyticsservice.providers.dispatcher;

import com.bioinformatics.analyticsservice.interfaces.AnalyticsService;
import com.bioinformatics.analyticsservice.models.*;
import com.bioinformatics.common.providers.ProviderContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnalyticsServiceDispatcherTest {

    @AfterEach
    void tearDown() {
        ProviderContextHolder.clear();
    }

    @Test
    void resolvesActiveProviderAndDelegatesAllAnalyticsCalls() {
        var postgres = new StubAnalyticsService("postgres");
        var mongo = new StubAnalyticsService("mongo");
        var dispatcher = new AnalyticsServiceDispatcher(List.of(postgres, mongo));

        ProviderContextHolder.set("postgres");

        assertThat(dispatcher.getDashboardKpis()).isSameAs(postgres.dashboardKpis);
        assertThat(dispatcher.getLengthHistogram()).containsExactlyElementsOf(postgres.histogram);
        assertThat(dispatcher.getByOrganism(7)).containsExactlyElementsOf(postgres.organisms);
        assertThat(dispatcher.getReviewedRatio()).containsExactlyElementsOf(postgres.reviewedRatios);
        assertThat(dispatcher.getEvidenceLevels()).containsExactlyElementsOf(postgres.evidenceLevels);
        assertThat(dispatcher.getKeywordFrequency(5)).containsExactlyElementsOf(postgres.keywords);
    }

    @Test
    void throwsWhenNoProviderIsActive() {
        var dispatcher = new AnalyticsServiceDispatcher(List.of(new StubAnalyticsService("postgres")));

        assertThatThrownBy(dispatcher::getDashboardKpis)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Lookup key cannot be null or blank");
    }

    private static final class StubAnalyticsService implements AnalyticsService {
        private final String providerName;
        private final DashboardKpisDto dashboardKpis = new DashboardKpisDto(42L, 10L, 32L, 5, 2, 120, 6000L, 30, 500);
        private final List<LengthHistogramBucketDto> histogram = List.of(new LengthHistogramBucketDto(1, 0, 99, 5));
        private final List<OrganismCountDto> organisms = List.of(new OrganismCountDto("Homo sapiens", 9606, 10, 8, 2, 120));
        private final List<ReviewedRatioDto> reviewedRatios = List.of(new ReviewedRatioDto(true, 10L));
        private final List<EvidenceDistributionDto> evidenceLevels = List.of(new EvidenceDistributionDto(1, "Experimental", 15L));
        private final List<KeywordFrequencyDto> keywords = List.of(new KeywordFrequencyDto("kinase", 7));

        private StubAnalyticsService(String providerName) {
            this.providerName = providerName;
        }

        @Override
        public String getProviderName() {
            return providerName;
        }

        @Override
        public DashboardKpisDto getDashboardKpis() {
            return dashboardKpis;
        }

        @Override
        public List<LengthHistogramBucketDto> getLengthHistogram() {
            return histogram;
        }

        @Override
        public List<OrganismCountDto> getByOrganism(int limit) {
            return organisms;
        }

        @Override
        public List<ReviewedRatioDto> getReviewedRatio() {
            return reviewedRatios;
        }

        @Override
        public List<EvidenceDistributionDto> getEvidenceLevels() {
            return evidenceLevels;
        }

        @Override
        public List<KeywordFrequencyDto> getKeywordFrequency(int limit) {
            return keywords;
        }
    }
}
