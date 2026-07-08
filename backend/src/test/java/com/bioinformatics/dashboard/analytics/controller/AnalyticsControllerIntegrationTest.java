package com.bioinformatics.dashboard.analytics.controller;

import com.bioinformatics.dashboard.admin.service.ImportService;
import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.auth.repository.AppUserRepository;
import com.bioinformatics.dashboard.exception.ErrorResponse;
import com.bioinformatics.dashboard.job.uniprot.fileloader.AsyncUniprotImportJobExecutor;
import com.bioinformatics.dashboard.model.analytics.*;
import com.bioinformatics.dashboard.providers.postgres.analytics.service.PostgresAnalyticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "app.rate-limiter.enabled=false")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureRestTestClient
class AnalyticsControllerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoSpyBean
    private PostgresAnalyticsService postgresAnalyticsService;

    @MockitoBean
    private ImportService importService;

    @MockitoBean
    private AsyncUniprotImportJobExecutor asyncUniprotImportJobExecutor;

    @TestConfiguration
    @Profile("test")
    static class CacheTestConfig {
        @Bean
        @Primary
        public CacheManager cacheManager() {
            return new NoOpCacheManager();
        }
    }

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        var user = AppUser.builder()
                .username("analytics_user")
                .password(passwordEncoder.encode("analytics_pass"))
                .role("ROLE_USER")
                .build();
        userRepository.saveAndFlush(user);

        userToken = obtainToken();
    }

    @Test
    void getDashboardKpis_returnsExpectedContractShape() {
        var dto = new DashboardKpisDto(1000L, 600L, 400L, 80, 80, 360, 40643L, 2, 35213);
        doReturn(dto).when(postgresAnalyticsService).getDashboardKpis();

        restClient.get()
                .uri("/api/analytics/dashboard-kpis")
                .header("Authorization", "Bearer " + userToken)
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
                .uri("/api/analytics/length-histogram")
                .header("Authorization", "Bearer " + userToken)
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
                .uri("/api/analytics/by-organism?limit=1")
                .header("Authorization", "Bearer " + userToken)
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
                .uri("/api/analytics/reviewed-ratio")
                .header("Authorization", "Bearer " + userToken)
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
                .uri("/api/analytics/evidence-levels")
                .header("Authorization", "Bearer " + userToken)
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
                .uri("/api/analytics/keyword-frequency?limit=1")
                .header("Authorization", "Bearer " + userToken)
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
                .uri("/api/analytics/by-organism?limit=201")
                .header("Authorization", "Bearer " + userToken)
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
                .uri("/api/analytics/keyword-frequency?limit=501")
                .header("Authorization", "Bearer " + userToken)
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

    private String obtainToken() throws Exception {
        var payload = objectMapper.writeValueAsString(Map.of("username", "analytics_user", "password", "analytics_pass"));
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        var body = result.getResponse().getContentAsString();
        var json = objectMapper.readTree(body);
        return json.get("accessToken").asText();
    }
}

