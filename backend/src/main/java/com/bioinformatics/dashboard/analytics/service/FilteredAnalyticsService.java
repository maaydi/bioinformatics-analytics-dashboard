package com.bioinformatics.dashboard.analytics.service;

import com.bioinformatics.dashboard.analytics.dto.*;
import com.bioinformatics.dashboard.gene.dto.GeneSearchRequest;
import com.bioinformatics.dashboard.gene.repository.ProteinEntryRepository;
import com.bioinformatics.dashboard.gene.specification.GeneSpecification;
import lombok.RequiredArgsConstructor;
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
public class FilteredAnalyticsService {

    private final ProteinEntryRepository proteinEntryRepository;


    public DashboardKpisDto getDashboardKpis(GeneSearchRequest request) {
        var spec = GeneSpecification.fromRequest(request);
        return proteinEntryRepository.getDashboardKpis(spec);
    }

    public List<LengthHistogramBucketDto> getLengthHistogram(GeneSearchRequest request) {
        var spec = GeneSpecification.fromRequest(request);
        return proteinEntryRepository.getLengthHistogram(spec);
    }

    public List<OrganismCountDto> getByOrganism(int limit, GeneSearchRequest request) {
        var spec = GeneSpecification.fromRequest(request);
        return proteinEntryRepository.getByOrganism(limit, spec);
    }

    public List<ReviewedRatioDto> getReviewedRatio(GeneSearchRequest request) {
        var spec = GeneSpecification.fromRequest(request);
        return proteinEntryRepository.getReviewedRatio(spec);
    }

    public List<EvidenceDistributionDto> getEvidenceLevels(GeneSearchRequest request) {
        var spec = GeneSpecification.fromRequest(request);
        return proteinEntryRepository.getEvidenceLevels(spec);
    }

    public List<KeywordFrequencyDto> getKeywordFrequency(int limit, GeneSearchRequest request) {
        var spec = GeneSpecification.fromRequest(request);
        return proteinEntryRepository.getKeywordFrequency(limit, spec);
    }
}
