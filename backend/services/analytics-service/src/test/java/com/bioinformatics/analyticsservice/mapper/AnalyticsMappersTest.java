package com.bioinformatics.analyticsservice.mapper;

import com.bioinformatics.analyticsservice.models.*;
import com.bioinformatics.analyticsservice.providers.postgres.entity.*;
import com.bioinformatics.analyticsservice.providers.postgres.mapper.*;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsMappersTest {

    @Test
    void dashboardKpisMapper_mapsFieldsAndComputesUnreviewedCount() {
        var mapper = Mappers.getMapper(DashboardKpisMapper.class);
        var entity = mock(DashboardKpis.class);
        when(entity.getTotalProteins()).thenReturn(1000L);
        when(entity.getReviewedCount()).thenReturn(600L);
        when(entity.getOrganismCount()).thenReturn(42);
        when(entity.getTaxonCount()).thenReturn(42);
        when(entity.getAvgLength()).thenReturn(360);
        when(entity.getAvgMolecularWeight()).thenReturn(40643L);
        when(entity.getMinLength()).thenReturn(2);
        when(entity.getMaxLength()).thenReturn(35213);

        var result = mapper.toDto(entity);

        assertThat(result).isEqualTo(new DashboardKpisDto(1000L, 600L, 400L, 42, 42, 360, 40643L, 2, 35213));
    }

    @Test
    void lengthHistogramMapper_mapsEntity() {
        var mapper = Mappers.getMapper(LengthHistogramBucketMapper.class);
        var entity = mock(LengthHistogramBucket.class);
        when(entity.getBucket()).thenReturn(2);
        when(entity.getRangeMin()).thenReturn(100);
        when(entity.getRangeMax()).thenReturn(199);
        when(entity.getCount()).thenReturn(12L);

        var result = mapper.toDto(entity);

        assertThat(result).isEqualTo(new LengthHistogramBucketDto(2, 100, 199, 12));
    }

    @Test
    void organismCountMapper_mapsEntity() {
        var mapper = Mappers.getMapper(OrganismCountMapper.class);
        var entity = mock(OrganismCount.class);
        when(entity.getOrganismName()).thenReturn("Homo sapiens (Human)");
        when(entity.getTaxid()).thenReturn(9606);
        when(entity.getTotal()).thenReturn(20581);
        when(entity.getReviewedCount()).thenReturn(20581);
        when(entity.getUnreviewedCount()).thenReturn(0);
        when(entity.getAvgLength()).thenReturn(480);

        var result = mapper.toDto(entity);

        assertThat(result).isEqualTo(new OrganismCountDto("Homo sapiens (Human)", 9606, 20581, 20581, 0, 480));
    }

    @Test
    void reviewedRatioMapper_mapsEntity() {
        var mapper = Mappers.getMapper(ReviewedRatioMapper.class);
        var entity = mock(ReviewedRatio.class);
        when(entity.getReviewed()).thenReturn(true);
        when(entity.getCount()).thenReturn(570000L);

        var result = mapper.toDto(entity);

        assertThat(result).isEqualTo(new ReviewedRatioDto(true, 570000L));
    }

    @Test
    void evidenceDistributionMapper_mapsEntity() {
        var mapper = Mappers.getMapper(EvidenceDistributionMapper.class);
        var entity = mock(EvidenceDistribution.class);
        when(entity.getEvidenceLevel()).thenReturn((short) 1);
        when(entity.getLabel()).thenReturn("Protein level");
        when(entity.getCount()).thenReturn(400000L);

        var result = mapper.toDto(entity);

        assertThat(result).isEqualTo(new EvidenceDistributionDto(1, "Protein level", 400000L));
    }

    @Test
    void keywordFrequencyMapper_mapsEntity() {
        var mapper = Mappers.getMapper(KeywordFrequencyMapper.class);
        var entity = mock(KeywordFrequency.class);
        when(entity.getKeyword()).thenReturn("Kinase");
        when(entity.getCount()).thenReturn(18000L);

        var result = mapper.toDto(entity);

        assertThat(result).isEqualTo(new KeywordFrequencyDto("Kinase", 18000L));
    }
}

