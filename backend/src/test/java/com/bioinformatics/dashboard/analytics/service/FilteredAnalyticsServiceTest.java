package com.bioinformatics.dashboard.analytics.service;

import com.bioinformatics.dashboard.gene.repository.ProteinEntryRepository;
import com.bioinformatics.dashboard.model.analytics.*;
import com.bioinformatics.dashboard.model.analytics.compare.AnalyticsSubsetDto;
import com.bioinformatics.dashboard.model.analytics.compare.CompareRequestDto;
import com.bioinformatics.dashboard.model.gene.GeneSearchRequest;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class FilteredAnalyticsServiceTest {

    @Mock
    private ProteinEntryRepository proteinEntryRepository;

    @InjectMocks
    private FilteredAnalyticsService filteredAnalyticsService;

    private GeneSearchRequest blankRequest;

    private static @NonNull CompareRequestDto getCompareRequestDto() {
        var requestA = new GeneSearchRequest(
                null, "ACC123", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null
        );
        var requestB = new GeneSearchRequest(
                null, "ACC456", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null
        );
        return new CompareRequestDto(requestA, requestB);
    }

    @BeforeEach
    void setUp() {
        blankRequest = new GeneSearchRequest(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null
        );
    }

    @Test
    @DisplayName("getDashboardKpis should delegate to repository and return KPIs")
    void getDashboardKpis_ShouldReturnKpis() {
        var expectedKpis = mock(DashboardKpisDto.class);
        when(proteinEntryRepository.getDashboardKpis(any(Specification.class))).thenReturn(expectedKpis);

        var result = filteredAnalyticsService.getDashboardKpis(blankRequest);

        assertNotNull(result);
        assertEquals(expectedKpis, result);
        verify(proteinEntryRepository, times(1)).getDashboardKpis(any(Specification.class));
    }

    @Test
    @DisplayName("getLengthHistogram should delegate to repository and return bucket list")
    void getLengthHistogram_ShouldReturnBuckets() {
        List<LengthHistogramBucketDto> expectedHistogram = Collections.emptyList();
        when(proteinEntryRepository.getLengthHistogram(any(Specification.class))).thenReturn(expectedHistogram);

        var result = filteredAnalyticsService.getLengthHistogram(blankRequest);

        assertNotNull(result);
        assertEquals(expectedHistogram, result);
        verify(proteinEntryRepository, times(1)).getLengthHistogram(any(Specification.class));
    }

    @Test
    @DisplayName("getByOrganism should delegate to repository with correct limit constraint")
    void getByOrganism_ShouldReturnOrganismCounts() {
        int limit = 10;
        List<OrganismCountDto> expectedCounts = Collections.emptyList();
        when(proteinEntryRepository.getByOrganism(eq(limit), any(Specification.class))).thenReturn(expectedCounts);

        var result = filteredAnalyticsService.getByOrganism(limit, blankRequest);

        assertNotNull(result);
        assertEquals(expectedCounts, result);
        verify(proteinEntryRepository, times(1)).getByOrganism(eq(limit), any(Specification.class));
    }

    @Test
    @DisplayName("getReviewedRatio should delegate to repository and return ratios")
    void getReviewedRatio_ShouldReturnRatios() {
        List<ReviewedRatioDto> expectedRatios = Collections.emptyList();
        when(proteinEntryRepository.getReviewedRatio(any(Specification.class))).thenReturn(expectedRatios);

        var result = filteredAnalyticsService.getReviewedRatio(blankRequest);

        assertNotNull(result);
        assertEquals(expectedRatios, result);
        verify(proteinEntryRepository, times(1)).getReviewedRatio(any(Specification.class));
    }

    @Test
    @DisplayName("getEvidenceLevels should delegate to repository and return distribution")
    void getEvidenceLevels_ShouldReturnDistribution() {
        List<EvidenceDistributionDto> expectedDistribution = Collections.emptyList();
        when(proteinEntryRepository.getEvidenceLevels(any(Specification.class))).thenReturn(expectedDistribution);

        var result = filteredAnalyticsService.getEvidenceLevels(blankRequest);

        assertNotNull(result);
        assertEquals(expectedDistribution, result);
        verify(proteinEntryRepository, times(1)).getEvidenceLevels(any(Specification.class));
    }

    @Test
    @DisplayName("getKeywordFrequency should delegate to repository with limit constraint")
    void getKeywordFrequency_ShouldReturnFrequencyList() {
        int limit = 5;
        List<KeywordFrequencyDto> expectedFrequencies = Collections.emptyList();
        when(proteinEntryRepository.getKeywordFrequency(eq(limit), any(Specification.class))).thenReturn(expectedFrequencies);

        var result = filteredAnalyticsService.getKeywordFrequency(limit, blankRequest);

        assertNotNull(result);
        assertEquals(expectedFrequencies, result);
        verify(proteinEntryRepository, times(1)).getKeywordFrequency(eq(limit), any(Specification.class));
    }

    @Test
    @DisplayName("getProteinLengthWeightCount should delegate to repository and return structural count metrics")
    void getProteinLengthWeightCount_ShouldReturnMetrics() {
        List<ProteinLengthWeightCount> expectedMetrics = Collections.emptyList();
        when(proteinEntryRepository.getProteinLengthWeightCount(any(Specification.class))).thenReturn(expectedMetrics);

        var result = filteredAnalyticsService.getProteinLengthWeightCount(blankRequest);

        assertNotNull(result);
        assertEquals(expectedMetrics, result);
        verify(proteinEntryRepository, times(1)).getProteinLengthWeightCount(any(Specification.class));
    }

    @Test
    @DisplayName("compare should separately resolve Specifications for setA and setB, returning combined responses")
    void compare_ShouldEvaluateBothSubsetsAndReturnComparison() {
        var compareRequestDto = getCompareRequestDto();

        var mockSubsetA = mock(AnalyticsSubsetDto.class);
        var mockSubsetB = mock(AnalyticsSubsetDto.class);

        when(proteinEntryRepository.getAnalyticsSubset(any(Specification.class)))
                .thenReturn(mockSubsetA)
                .thenReturn(mockSubsetB);

        var response = filteredAnalyticsService.compare(compareRequestDto);

        assertNotNull(response);
        assertEquals(mockSubsetA, response.subsetA());
        assertEquals(mockSubsetB, response.subsetB());

        verify(proteinEntryRepository, times(2)).getAnalyticsSubset(any(Specification.class));
    }
}