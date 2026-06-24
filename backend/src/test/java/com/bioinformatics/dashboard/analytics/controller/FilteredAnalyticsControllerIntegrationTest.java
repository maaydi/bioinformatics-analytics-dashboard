package com.bioinformatics.dashboard.analytics.controller;

import com.bioinformatics.dashboard.admin.service.ImportService;
import com.bioinformatics.dashboard.analytics.dto.*;
import com.bioinformatics.dashboard.analytics.dto.compare.AnalyticsSubsetDto;
import com.bioinformatics.dashboard.analytics.dto.compare.CompareRequestDto;
import com.bioinformatics.dashboard.analytics.dto.compare.CompareResponseDto;
import com.bioinformatics.dashboard.analytics.service.FilteredAnalyticsService;
import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.auth.repository.AppUserRepository;
import com.bioinformatics.dashboard.batch.AsyncUniprotImportJobExecutor;
import com.bioinformatics.dashboard.exception.ErrorResponse;
import com.bioinformatics.dashboard.gene.dto.GeneSearchRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = "app.rate-limiter.enabled=false")
@AutoConfigureMockMvc
@AutoConfigureRestTestClient
class FilteredAnalyticsControllerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    // A valid empty/default request payload to satisfy canonical constructor properties
    private final GeneSearchRequest emptySearchRequest = new GeneSearchRequest(
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null
    );
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RestTestClient restClient;
    @Autowired
    private AppUserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @MockitoBean
    private FilteredAnalyticsService analyticsService;
    @MockitoBean
    private ImportService importService;
    @MockitoBean
    private AsyncUniprotImportJobExecutor asyncUniprotImportJobExecutor;
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
        when(analyticsService.getDashboardKpis(any(GeneSearchRequest.class))).thenReturn(dto);

        restClient.post()
                .uri("/api/analytics/filters/dashboard-kpis")
                .header("Authorization", "Bearer " + userToken)
                .body(emptySearchRequest)
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
        var dto = new LengthHistogramBucketDto(1, 0L, 100L, 12000L);
        when(analyticsService.getLengthHistogram(any(GeneSearchRequest.class))).thenReturn(List.of(dto));

        restClient.post()
                .uri("/api/analytics/filters/length-histogram")
                .header("Authorization", "Bearer " + userToken)
                .body(emptySearchRequest)
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
        when(analyticsService.getByOrganism(eq(50), any(GeneSearchRequest.class))).thenReturn(List.of(dto));

        restClient.post()
                .uri("/api/analytics/filters/by-organism?limit=50")
                .header("Authorization", "Bearer " + userToken)
                .body(emptySearchRequest)
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

        when(analyticsService.getReviewedRatio(any(GeneSearchRequest.class))).thenReturn(List.of(ratio));
        when(analyticsService.getEvidenceLevels(any(GeneSearchRequest.class))).thenReturn(List.of(evidence));

        restClient.post()
                .uri("/api/analytics/filters/reviewed-ratio")
                .header("Authorization", "Bearer " + userToken)
                .body(emptySearchRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<ReviewedRatioDto>>() {
                })
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body).containsExactly(ratio);
                });

        restClient.post()
                .uri("/api/analytics/filters/evidence-levels")
                .header("Authorization", "Bearer " + userToken)
                .body(emptySearchRequest)
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
        when(analyticsService.getKeywordFrequency(eq(100), any(GeneSearchRequest.class))).thenReturn(List.of(dto));

        restClient.post()
                .uri("/api/analytics/filters/keyword-frequency?limit=100")
                .header("Authorization", "Bearer " + userToken)
                .body(emptySearchRequest)
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
    void getProteinLengthWeightCount_returnsExpectedList() {
        // Since fields are dynamic, an empty tracking list safely validates the response structure
        when(analyticsService.getProteinLengthWeightCount(any(GeneSearchRequest.class))).thenReturn(List.of());

        restClient.post()
                .uri("/api/analytics/filters/length-weight")
                .header("Authorization", "Bearer " + userToken)
                .body(emptySearchRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<ProteinLengthWeightCount>>() {
                })
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body).isEmpty();
                });
    }

    @Test
    void compare_returnsExpectedSubsets() {
        var compareRequest = new CompareRequestDto(emptySearchRequest, emptySearchRequest);
        var subsetA = new AnalyticsSubsetDto(10L, 500L, 8L, 80L, List.of(), List.of());
        var subsetB = new AnalyticsSubsetDto(20L, 400L, 10L, 50L, List.of(), List.of());
        var responseDto = new CompareResponseDto(subsetA, subsetB);

        when(analyticsService.compare(any(CompareRequestDto.class))).thenReturn(responseDto);

        restClient.post()
                .uri("/api/analytics/filters/compare")
                .header("Authorization", "Bearer " + userToken)
                .body(compareRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CompareResponseDto.class)
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.subsetA().count()).isEqualTo(10L);
                    assertThat(body.subsetB().count()).isEqualTo(20L);
                });
    }

    @Test
    void getByOrganism_withInvalidLimit_returnsBadRequestWithoutServiceCall() {
        restClient.post()
                .uri("/api/analytics/filters/by-organism?limit=201") // Limit max is 200
                .header("Authorization", "Bearer " + userToken)
                .body(emptySearchRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.status()).isEqualTo(400);
                    assertThat(body.message()).contains("Limit should be lower than 201");
                });

        verifyNoInteractions(analyticsService);
    }

    @Test
    void getKeywordFrequency_withInvalidLimit_returnsBadRequestWithoutServiceCall() {
        restClient.post()
                .uri("/api/analytics/filters/keyword-frequency?limit=501") // Limit max is 500
                .header("Authorization", "Bearer " + userToken)
                .body(emptySearchRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.status()).isEqualTo(400);
                    assertThat(body.message()).contains("Limit should be lower than 501");
                });

        verifyNoInteractions(analyticsService);
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