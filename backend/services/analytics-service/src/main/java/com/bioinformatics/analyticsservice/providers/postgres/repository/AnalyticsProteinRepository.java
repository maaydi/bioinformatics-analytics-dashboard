package com.bioinformatics.analyticsservice.providers.postgres.repository;

import com.bioinformatics.analyticsservice.models.*;
import com.bioinformatics.analyticsservice.models.compare.AnalyticsSubsetDto;
import com.bioinformatics.common.gene.entity.ProteinEntry;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Defines dynamically evaluated analytics queries spanning multiple criteria.
 * Implementations should parse JPA specifications to render accurate runtime distributions.
 */
public interface AnalyticsProteinRepository {
    DashboardKpisDto getDashboardKpis(Specification<ProteinEntry> spec);

    List<LengthHistogramBucketDto> getLengthHistogram(Specification<ProteinEntry> spec);

    List<OrganismCountDto> getByOrganism(int limit, Specification<ProteinEntry> spec);

    List<ReviewedRatioDto> getReviewedRatio(Specification<ProteinEntry> spec);

    List<EvidenceDistributionDto> getEvidenceLevels(Specification<ProteinEntry> spec);

    List<KeywordFrequencyDto> getKeywordFrequency(int limit, Specification<ProteinEntry> spec);

    List<ProteinLengthWeightCount> getProteinLengthWeightCount(Specification<ProteinEntry> spec);

    AnalyticsSubsetDto getAnalyticsSubset(Specification<ProteinEntry> spec);

}
