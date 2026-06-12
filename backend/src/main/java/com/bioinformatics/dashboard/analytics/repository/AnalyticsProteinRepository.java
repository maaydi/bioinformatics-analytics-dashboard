package com.bioinformatics.dashboard.analytics.repository;

import com.bioinformatics.dashboard.analytics.dto.*;
import com.bioinformatics.dashboard.analytics.dto.compare.AnalyticsSubsetDto;
import com.bioinformatics.dashboard.gene.entity.ProteinEntry;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

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
