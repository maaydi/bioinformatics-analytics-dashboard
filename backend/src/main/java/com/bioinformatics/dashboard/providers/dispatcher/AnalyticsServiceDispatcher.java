package com.bioinformatics.dashboard.providers.dispatcher;

import com.bioinformatics.dashboard.interfaces.analytics.AnalyticsService;
import com.bioinformatics.dashboard.model.analytics.*;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Primary
public class AnalyticsServiceDispatcher extends AbstractProviderDispatcher<AnalyticsService> implements AnalyticsService {

    public AnalyticsServiceDispatcher(List<AnalyticsService> services) {
        super(services);
    }


    @Override
    public DashboardKpisDto getDashboardKpis() {
        return resolve().getDashboardKpis();
    }

    @Override
    public List<LengthHistogramBucketDto> getLengthHistogram() {
        return resolve().getLengthHistogram();
    }

    @Override
    public List<OrganismCountDto> getByOrganism(int limit) {
        return resolve().getByOrganism(limit);
    }

    @Override
    public List<ReviewedRatioDto> getReviewedRatio() {
        return resolve().getReviewedRatio();
    }

    @Override
    public List<EvidenceDistributionDto> getEvidenceLevels() {
        return resolve().getEvidenceLevels();
    }

    @Override
    public List<KeywordFrequencyDto> getKeywordFrequency(int limit) {
        return resolve().getKeywordFrequency(limit);
    }

}
