package com.bioinformatics.analyticsservice.controller;

import com.bioinformatics.analyticsservice.models.*;
import com.bioinformatics.analyticsservice.providers.postgres.service.PostgresAnalyticsService;
import com.bioinformatics.common.exception.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;

import static com.bioinformatics.shared.models.security.Constants.USER_ID_HEADER;
import static com.bioinformatics.shared.models.security.Constants.USER_ROLE_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureRestTestClient
class AnalyticsControllerIntegrationTest {

    @Autowired
    private RestTestClient restClient;

    @MockitoSpyBean
    private PostgresAnalyticsService postgresAnalyticsService;

    @TestConfiguration
    static class CacheTestConfig {
        @Bean
        @Primary
        public CacheManager cacheManager() {
            return new NoOpCacheManager();
        }
    }

    @Test
    void getDashboardKpis_returnsExpectedContractShape() {
        var dto = new DashboardKpisDto(1000L, 600L, 400L, 80, 80, 360, 40643L, 2, 35213);
        doReturn(dto).when(postgresAnalyticsService).getDashboardKpis();

        restClient.get()
                .uri("/api/v1/analytics/dashboard-kpis")
                .header(USER_ID_HEADER, "analytics_user")
                .header(USER_ROLE_HEADER, "USER")
                .exchange()
                .expectStatus().isOk()
                .expectBody(DashboardKpisDto.class)
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.totalProteins()).isEqualTo(1000L);
                    assertThat(body.unreviewedCount()).isEqualTo(400L);
                    assertThat(body.maxLength()).isEqualTo(35213);
                });
    }

    @Test
    void getLengthHistogram_returnsBucketList() {
        var dto = new LengthHistogramBucketDto(1, 0, 99, 12000);
        when(postgresAnalyticsService.getLengthHistogram()).thenReturn(List.of(dto));

        restClient.get()
                .uri("/api/v1/analytics/length-histogram")
                .header(USER_ID_HEADER, "analytics_user")
                .header(USER_ROLE_HEADER, "USER")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<LengthHistogramBucketDto>>() {
                })
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body).containsExactly(dto);
                });
    }

    @Test
    void getByOrganism_withValidLimit_returnsTopOrganisms() {
        var dto = new OrganismCountDto("Homo sapiens (Human)", 9606, 20581, 20581, 0, 480);
        when(postgresAnalyticsService.getByOrganism(1)).thenReturn(List.of(dto));

        restClient.get()
                .uri("/api/v1/analytics/by-organism?limit=1")
                .header(USER_ID_HEADER, "analytics_user")
                .header(USER_ROLE_HEADER, "USER")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<OrganismCountDto>>() {
                })
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body).containsExactly(dto);
                });
    }

    @Test
    void getReviewedRatioAndEvidenceLevels_returnExpectedCollections() {
        var ratio = new ReviewedRatioDto(true, 570000L);
        var evidence = new EvidenceDistributionDto(1, "Protein level", 400000L);
        when(postgresAnalyticsService.getReviewedRatio()).thenReturn(List.of(ratio));
        when(postgresAnalyticsService.getEvidenceLevels()).thenReturn(List.of(evidence));

        restClient.get()
                .uri("/api/v1/analytics/reviewed-ratio")
                .header(USER_ID_HEADER, "analytics_user")
                .header(USER_ROLE_HEADER, "USER")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<ReviewedRatioDto>>() {
                })
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body).containsExactly(ratio);
                });

        restClient.get()
                .uri("/api/v1/analytics/evidence-levels")
                .header(USER_ID_HEADER, "analytics_user")
                .header(USER_ROLE_HEADER, "USER")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<EvidenceDistributionDto>>() {
                })
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body).containsExactly(evidence);
                });
    }

    @Test
    void getKeywordFrequency_withValidLimit_returnsRankedKeywords() {
        var dto = new KeywordFrequencyDto("Kinase", 18000L);
        when(postgresAnalyticsService.getKeywordFrequency(1)).thenReturn(List.of(dto));

        restClient.get()
                .uri("/api/v1/analytics/keyword-frequency?limit=1")
                .header(USER_ID_HEADER, "analytics_user")
                .header(USER_ROLE_HEADER, "USER")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<KeywordFrequencyDto>>() {
                })
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body).containsExactly(dto);
                });
    }

    @Test
    void getByOrganism_withInvalidLimit_returnsBadRequestWithoutServiceCall() {
        restClient.get()
                .uri("/api/v1/analytics/by-organism?limit=201")
                .header(USER_ID_HEADER, "analytics_user")
                .header(USER_ROLE_HEADER, "USER")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.status()).isEqualTo(400);
                    assertThat(body.message()).contains("Limit should be lower than 201");
                });

        verifyNoInteractions(postgresAnalyticsService);
    }

    @Test
    void getKeywordFrequency_withInvalidLimit_returnsBadRequestWithoutServiceCall() {
        restClient.get()
                .uri("/api/v1/analytics/keyword-frequency?limit=501")
                .header(USER_ID_HEADER, "analytics_user")
                .header(USER_ROLE_HEADER, "USER")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.status()).isEqualTo(400);
                    assertThat(body.message()).contains("Limit should be lower than 501");
                });

        verifyNoInteractions(postgresAnalyticsService);
    }

}

