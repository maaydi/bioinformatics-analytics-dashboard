package com.bioinformatics.dashboard.gene;

import com.bioinformatics.dashboard.admin.service.ImportService;
import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.auth.repository.AppUserRepository;
import com.bioinformatics.dashboard.batch.AsyncUniprotImportJobExecutor;
import com.bioinformatics.dashboard.gene.dto.PagedResponse;
import com.bioinformatics.dashboard.gene.dto.ProteinSummaryDto;
import com.bioinformatics.dashboard.gene.entity.ProteinEntry;
import com.bioinformatics.dashboard.gene.repository.ProteinEntryRepository;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "app.rate-limiter.enabled=false")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureRestTestClient
class GeneControllerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    MockMvc mockMvc;
    @Autowired
    RestTestClient restClient;
    @Autowired
    ProteinEntryRepository proteinEntryRepository;
    @Autowired
    AppUserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @MockitoBean
    ImportService importService;
    @MockitoBean
    AsyncUniprotImportJobExecutor asyncUniprotImportJobExecutor;

    @TestConfiguration
    static class CacheTestConfig {
        @Bean
        @Primary
        public CacheManager cacheManager() {
            return new NoOpCacheManager();
        }
    }


    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        proteinEntryRepository.deleteAll();
        userRepository.deleteAll();

        // create admin
        var admin = AppUser.builder()
                .username("admin_user")
                .password(passwordEncoder.encode("admin_pass"))
                .role("ROLE_ADMIN")
                .build();
        userRepository.save(admin);
        userRepository.flush();
        adminToken = obtainToken("admin_user", "admin_pass");

        var user = AppUser.builder()
                .username("regular_user")
                .password(passwordEncoder.encode("user_pass"))
                .role("ROLE_USER")
                .build();
        userRepository.save(user);
        userRepository.flush();
        userToken = obtainToken("regular_user", "user_pass");
    }

    private String obtainToken(String username, String password) throws Exception {
        var payload = objectMapper.writeValueAsString(java.util.Map.of("username", username, "password", password));
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        var body = result.getResponse().getContentAsString();
        var node = objectMapper.readTree(body);
        return node.get("accessToken").asText();
    }

    @Test
    void getGenes_returnsPagedResponse() throws Exception {
        var entry = ProteinEntry.builder()
                .accession("ACC1")
                .entryName("entry1")
                .reviewed(true)
                .organismName("Org")
                .taxid(9606)
                .length(100)
                .evidenceLevel((short) 1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        proteinEntryRepository.save(entry);

        restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/genes")
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .build())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new org.springframework.core.ParameterizedTypeReference<PagedResponse<ProteinSummaryDto>>() {
                })
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.content()).hasSize(1);
                });
    }

    @Test
    void postSearch_returnsFilteredResults() throws Exception {
        var e1 = ProteinEntry.builder()
                .accession("A1")
                .entryName("alpha")
                .reviewed(true)
                .organismName("Org1")
                .taxid(111)
                .length(120)
                .evidenceLevel((short) 1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        var e2 = ProteinEntry.builder()
                .accession("B2")
                .entryName("beta")
                .reviewed(true)
                .organismName("Org2")
                .taxid(222)
                .length(80)
                .evidenceLevel((short) 1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        proteinEntryRepository.saveAll(List.of(e1, e2));

        var request = java.util.Map.<String, Object>of("organism", "Org1", "page", 0, "size", 10);
        restClient.post()
                .uri("/api/genes/search")
                .header("Authorization", "Bearer " + userToken)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PagedResponse<ProteinSummaryDto>>() {
                })
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.content()).hasSize(1);
                    assertThat(body.content().getFirst().organismName()).isEqualTo("Org1");
                });
    }

    @Test
    void getGeneById_returnsDetail() throws Exception {
        var entry = ProteinEntry.builder()
                .accession("ID123")
                .entryName("entryX")
                .reviewed(false)
                .organismName("OrgX")
                .taxid(333)
                .length(50)
                .evidenceLevel((short) 2)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        var saved = proteinEntryRepository.save(entry);

        restClient.get()
                .uri("/api/genes/{id}", saved.getId())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                });
    }

    @Test
    void getGeneById_notFound_returnsNotFound() throws Exception {
        restClient.get()
                .uri("/api/genes/{id}", 9999)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getGenes_invalidSort_returnsBadRequest() throws Exception {
        restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/genes").queryParam("sort", "not_a_field").build())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void postSearch_lengthRangeInvalid_returnsBadRequest() throws Exception {
        var request = java.util.Map.<String, Object>of("lengthMin", 200, "lengthMax", 10, "page", 0, "size", 10);
        restClient.post()
                .uri("/api/genes/search")
                .header("Authorization", "Bearer " + userToken)
                .body(request)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void postExportCsv_returnsCsv() throws Exception {
        var entry = ProteinEntry.builder()
                .accession("ECX1")
                .entryName("csv1")
                .reviewed(true)
                .organismName("OrgCsv")
                .taxid(999)
                .length(10)
                .evidenceLevel((short) 1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        proteinEntryRepository.save(entry);

        var request = java.util.Map.<String, Object>of("page", 0, "size", 10);
        restClient.post()
                .uri("/api/genes/export-csv")
                .header("Authorization", "Bearer " + adminToken)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body).contains("accession");
                });
    }

}

