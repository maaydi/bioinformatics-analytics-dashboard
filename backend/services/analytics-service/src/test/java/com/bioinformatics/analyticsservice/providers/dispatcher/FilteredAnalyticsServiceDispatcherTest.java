package com.bioinformatics.analyticsservice.providers.dispatcher;

import com.bioinformatics.analyticsservice.interfaces.FilteredAnalyticsService;
import com.bioinformatics.analyticsservice.models.*;
import com.bioinformatics.analyticsservice.models.compare.AnalyticsSubsetDto;
import com.bioinformatics.analyticsservice.models.compare.CompareRequestDto;
import com.bioinformatics.analyticsservice.models.compare.CompareResponseDto;
import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.common.providers.ProviderContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilteredAnalyticsServiceDispatcherTest {

    @AfterEach
    void tearDown() {
        ProviderContextHolder.clear();
    }

    @Test
    void resolvesActiveProviderAndDelegatesAllFilteredMethods() {
        var postgres = new StubFilteredAnalyticsService("postgres");
        var mongo = new StubFilteredAnalyticsService("mongo");
        var dispatcher = new FilteredAnalyticsServiceDispatcher(List.of(postgres, mongo));
        var request = new GeneSearchRequest(null, "P12345", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null);

        ProviderContextHolder.set("postgres");

        assertThat(dispatcher.getDashboardKpis(request)).isSameAs(postgres.dashboardKpis);
        assertThat(dispatcher.getLengthHistogram(request)).containsExactlyElementsOf(postgres.histogram);
        assertThat(dispatcher.getByOrganism(4, request)).containsExactlyElementsOf(postgres.organisms);
        assertThat(dispatcher.getReviewedRatio(request)).containsExactlyElementsOf(postgres.reviewedRatios);
        assertThat(dispatcher.getEvidenceLevels(request)).containsExactlyElementsOf(postgres.evidenceLevels);
        assertThat(dispatcher.getKeywordFrequency(3, request)).containsExactlyElementsOf(postgres.keywords);
        assertThat(dispatcher.getProteinLengthWeightCount(request)).containsExactlyElementsOf(postgres.lengthWeightCounts);

        var compareRequest = new CompareRequestDto(request, request);
        assertThat(dispatcher.compare(compareRequest)).isEqualTo(new CompareResponseDto(postgres.subsetA, postgres.subsetB));
    }

    @Test
    void throwsWhenNoProviderIsActive() {
        var dispatcher = new FilteredAnalyticsServiceDispatcher(List.of(new StubFilteredAnalyticsService("postgres")));
        var request = new GeneSearchRequest(null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> dispatcher.getDashboardKpis(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Lookup key cannot be null or blank");
    }

    private static final class StubFilteredAnalyticsService implements FilteredAnalyticsService {
        private final String providerName;
        private final DashboardKpisDto dashboardKpis = new DashboardKpisDto(1L, 1L, 0L, 1, 1, 50, 10L, 10, 10);
        private final List<LengthHistogramBucketDto> histogram = List.of(new LengthHistogramBucketDto(1, 0, 9, 4));
        private final List<OrganismCountDto> organisms = List.of(new OrganismCountDto("Bacteria", 2, 4, 3, 1, 50));
        private final List<ReviewedRatioDto> reviewedRatios = List.of(new ReviewedRatioDto(true, 1L));
        private final List<EvidenceDistributionDto> evidenceLevels = List.of(new EvidenceDistributionDto(1, "Experimental", 2L));
        private final List<KeywordFrequencyDto> keywords = List.of(new KeywordFrequencyDto("signal", 6));
        private final List<ProteinLengthWeightCount> lengthWeightCounts = List.of(new ProteinLengthWeightCount(50, 250, 3));
        private final AnalyticsSubsetDto subsetA = new AnalyticsSubsetDto(1L, 1L, 1L, 1L, List.of(), List.of());
        private final AnalyticsSubsetDto subsetB = new AnalyticsSubsetDto(2L, 2L, 1L, 50L, List.of(), List.of());

        private StubFilteredAnalyticsService(String providerName) {
            this.providerName = providerName;
        }

        @Override
        public String getProviderName() {
            return providerName;
        }

        @Override
        public DashboardKpisDto getDashboardKpis(GeneSearchRequest request) {
            return dashboardKpis;
        }

        @Override
        public List<LengthHistogramBucketDto> getLengthHistogram(GeneSearchRequest request) {
            return histogram;
        }

        @Override
        public List<OrganismCountDto> getByOrganism(int limit, GeneSearchRequest request) {
            return organisms;
        }

        @Override
        public List<ReviewedRatioDto> getReviewedRatio(GeneSearchRequest request) {
            return reviewedRatios;
        }

        @Override
        public List<EvidenceDistributionDto> getEvidenceLevels(GeneSearchRequest request) {
            return evidenceLevels;
        }

        @Override
        public List<KeywordFrequencyDto> getKeywordFrequency(int limit, GeneSearchRequest request) {
            return keywords;
        }

        @Override
        public List<ProteinLengthWeightCount> getProteinLengthWeightCount(GeneSearchRequest request) {
            return lengthWeightCounts;
        }

        @Override
        public CompareResponseDto compare(CompareRequestDto request) {
            return new CompareResponseDto(subsetA, subsetB);
        }
    }
}
