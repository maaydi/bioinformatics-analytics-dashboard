package com.bioinformatics.analyticsservice.service;

import com.bioinformatics.analyticsservice.models.DashboardKpisDto;
import com.bioinformatics.analyticsservice.models.KeywordFrequencyDto;
import com.bioinformatics.analyticsservice.models.LengthHistogramBucketDto;
import com.bioinformatics.analyticsservice.models.OrganismCountDto;
import com.bioinformatics.analyticsservice.providers.postgres.entity.DashboardKpis;
import com.bioinformatics.analyticsservice.providers.postgres.entity.KeywordFrequency;
import com.bioinformatics.analyticsservice.providers.postgres.entity.LengthHistogramBucket;
import com.bioinformatics.analyticsservice.providers.postgres.entity.OrganismCount;
import com.bioinformatics.analyticsservice.providers.postgres.mapper.*;
import com.bioinformatics.analyticsservice.providers.postgres.repository.*;
import com.bioinformatics.analyticsservice.providers.postgres.service.PostgresAnalyticsService;
import com.bioinformatics.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostgresAnalyticsServiceTest {

    @Mock
    private DashboardKpisRepository dashboardKpisRepository;

    @Mock
    private DashboardKpisMapper dashboardKpisMapper;

    @Mock
    private LengthHistogramBucketRepository lengthHistogramBucketRepository;

    @Mock
    private LengthHistogramBucketMapper lengthHistogramBucketMapper;

    @Mock
    private OrganismCountRepository organismCountRepository;

    @Mock
    private OrganismCountMapper organismCountMapper;

    @Mock
    private ReviewedRatioRepository reviewedRatioRepository;

    @Mock
    private ReviewedRatioMapper reviewedRatioMapper;

    @Mock
    private EvidenceDistributionRepository evidenceDistributionRepository;

    @Mock
    private EvidenceDistributionMapper evidenceDistributionMapper;

    @Mock
    private KeywordFrequencyRepository keywordFrequencyRepository;

    @Mock
    private KeywordFrequencyMapper keywordFrequencyMapper;

    private PostgresAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new PostgresAnalyticsService(
                dashboardKpisRepository,
                dashboardKpisMapper,
                lengthHistogramBucketRepository,
                lengthHistogramBucketMapper,
                organismCountRepository,
                organismCountMapper,
                reviewedRatioRepository,
                reviewedRatioMapper,
                evidenceDistributionRepository,
                evidenceDistributionMapper,
                keywordFrequencyRepository,
                keywordFrequencyMapper
        );
    }

    @Test
    void getDashboardKpis_whenViewIsEmpty_throwsNotFound() {
        when(dashboardKpisRepository.findFirstBy()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDashboardKpis())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Dashboard KPIs not found");
    }

    @Test
    void getDashboardKpis_mapsRepositoryEntityToDto() {
        var entity = mock(DashboardKpis.class);
        var dto = new DashboardKpisDto(10L, 6L, 4L, 2, 2, 320, 44000L, 80, 900);
        when(dashboardKpisRepository.findFirstBy()).thenReturn(Optional.of(entity));
        when(dashboardKpisMapper.toDto(entity)).thenReturn(dto);

        var result = service.getDashboardKpis();

        assertThat(result).isEqualTo(dto);
        verify(dashboardKpisMapper).toDto(entity);
    }

    @Test
    void getLengthHistogram_mapsAllBuckets() {
        var first = mock(LengthHistogramBucket.class);
        var second = mock(LengthHistogramBucket.class);
        var firstDto = new LengthHistogramBucketDto(1, 0, 99, 3);
        var secondDto = new LengthHistogramBucketDto(2, 100, 199, 5);

        when(lengthHistogramBucketRepository.findAllByOrderByBucketAsc()).thenReturn(List.of(first, second));
        when(lengthHistogramBucketMapper.toDto(first)).thenReturn(firstDto);
        when(lengthHistogramBucketMapper.toDto(second)).thenReturn(secondDto);

        var result = service.getLengthHistogram();

        assertThat(result).containsExactly(firstDto, secondDto);
    }

    @Test
    void getByOrganism_usesRequestedLimitAndMapsDtos() {
        var entity = mock(OrganismCount.class);
        var dto = new OrganismCountDto("Homo sapiens", 9606, 100, 90, 10, 450);
        when(organismCountRepository.findAll(any(Limit.class))).thenReturn(List.of(entity));
        when(organismCountMapper.toDto(entity)).thenReturn(dto);

        var result = service.getByOrganism(25);

        var limitCaptor = ArgumentCaptor.forClass(Limit.class);
        verify(organismCountRepository).findAll(limitCaptor.capture());
        assertThat(limitCaptor.getValue().max()).isEqualTo(25);
        assertThat(result).containsExactly(dto);
    }

    @Test
    void getKeywordFrequency_usesRequestedLimitAndMapsDtos() {
        var entity = mock(KeywordFrequency.class);
        var dto = new KeywordFrequencyDto("Kinase", 42);
        when(keywordFrequencyRepository.findAll(any(Limit.class))).thenReturn(List.of(entity));
        when(keywordFrequencyMapper.toDto(entity)).thenReturn(dto);

        var result = service.getKeywordFrequency(100);

        var limitCaptor = ArgumentCaptor.forClass(Limit.class);
        verify(keywordFrequencyRepository).findAll(limitCaptor.capture());
        assertThat(limitCaptor.getValue().max()).isEqualTo(100);
        assertThat(result).containsExactly(dto);
    }
}

