package com.bioinformatics.dashboard.analytics.service;

import com.bioinformatics.dashboard.analytics.dto.*;
import com.bioinformatics.dashboard.analytics.dto.compare.CompareRequestDto;
import com.bioinformatics.dashboard.analytics.dto.compare.CompareResponseDto;
import com.bioinformatics.dashboard.gene.dto.GeneSearchRequest;
import com.bioinformatics.dashboard.gene.repository.ProteinEntryRepository;
import com.bioinformatics.dashboard.gene.specification.GeneSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for analytics queries.
 *
 * <p>All methods query pre-computed PostgreSQL materialized views.
 * Views are refreshed post-import by {@code UniProtImportJobConfig}.
 *
 * @see <a href="{@docRoot}/documentation/domain-model.md">Materialized Views</a>
 * @see <a href="{@docRoot}/documentation/api-contract.md">Analytics Endpoints</a>
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class FilteredAnalyticsService {

    private final ProteinEntryRepository proteinEntryRepository;


    public DashboardKpisDto getDashboardKpis(GeneSearchRequest request) {
        log.info("Retrieving Kpis for filtered analytics request: {}", request);
        var spec = GeneSpecification.fromRequest(request);
        return proteinEntryRepository.getDashboardKpis(spec);
    }

    public List<LengthHistogramBucketDto> getLengthHistogram(GeneSearchRequest request) {
        log.info("Retrieving length histogram for filtered analytics request: {}", request);
        var spec = GeneSpecification.fromRequest(request);
        return proteinEntryRepository.getLengthHistogram(spec);
    }

    public List<OrganismCountDto> getByOrganism(int limit, GeneSearchRequest request) {
        log.info("Retrieving organism count for filtered analytics request: {}", request);
        var spec = GeneSpecification.fromRequest(request);
        return proteinEntryRepository.getByOrganism(limit, spec);
    }

    public List<ReviewedRatioDto> getReviewedRatio(GeneSearchRequest request) {
        log.info("Retrieving reviewed ratio for filtered analytics request: {}", request);
        var spec = GeneSpecification.fromRequest(request);
        return proteinEntryRepository.getReviewedRatio(spec);
    }

    public List<EvidenceDistributionDto> getEvidenceLevels(GeneSearchRequest request) {
        log.info("Retrieving evidence levels for filtered analytics request: {}", request);
        var spec = GeneSpecification.fromRequest(request);
        return proteinEntryRepository.getEvidenceLevels(spec);
    }

    public List<KeywordFrequencyDto> getKeywordFrequency(int limit, GeneSearchRequest request) {
        log.info("Retrieving keyword frequency for filtered analytics request: {}", request);
        var spec = GeneSpecification.fromRequest(request);
        return proteinEntryRepository.getKeywordFrequency(limit, spec);
    }

    public List<ProteinLengthWeightCount> getProteinLengthWeightCount(GeneSearchRequest request) {
        log.info("Retrieving protein length frequency for filtered analytics request: {}", request);
        var spec = GeneSpecification.fromRequest(request);
        return proteinEntryRepository.getProteinLengthWeightCount(spec);
    }

    /**
     * Compares analytics aggregates for two filter sets.
     *
     * @param request contains two distinct GeneSearchRequest objects (setA, setB)
     * @return CompareResponseDto with AnalyticsSubsetDto for each set
     */
    public CompareResponseDto compare(CompareRequestDto request) {
        log.info("Compare two filter sets : Filter A : {} | Filter B : {}", request.setA(), request.setB());
        var specA = GeneSpecification.fromRequest(request.setA());
        var specB = GeneSpecification.fromRequest(request.setB());
        var setA = proteinEntryRepository.getAnalyticsSubset(specA);
        var setB = proteinEntryRepository.getAnalyticsSubset(specB);
        return new CompareResponseDto(setA, setB);

    }
}
