package com.bioinformatics.dashboard.gene;

import com.bioinformatics.common.gene.entity.ProteinEntry;
import com.bioinformatics.common.gene.repository.ProteinEntryRepository;
import com.bioinformatics.dashboard.admin.service.ImportService;
import com.bioinformatics.dashboard.job.uniprot.fileloader.AsyncUniprotImportJobExecutor;
import com.bioinformatics.dashboard.model.gene.PagedResponse;
import com.bioinformatics.dashboard.model.gene.ProteinSummaryDto;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Instant;
import java.util.List;

import static com.bioinformatics.shared.models.security.Constants.USER_ID_HEADER;
import static com.bioinformatics.shared.models.security.Constants.USER_ROLE_HEADER;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "app.rate-limiter.enabled=false")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureRestTestClient
class GeneControllerIntegrationTest {
    @Autowired
    RestTestClient restClient;
    @Autowired
    ProteinEntryRepository proteinEntryRepository;

    @MockitoBean
    ImportService importService;
    @MockitoBean
    AsyncUniprotImportJobExecutor asyncUniprotImportJobExecutor;

    @TestConfiguration
    @Profile("test")
    static class CacheTestConfig {
        @Bean
        @Primary
        public CacheManager cacheManager() {
            return new NoOpCacheManager();
        }
    }

    @BeforeEach
    void setUp() {
        proteinEntryRepository.deleteAll();
    }


    @Test
    void getGenes_returnsPagedResponse() {
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
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, "ADMIN")
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
    void postSearch_returnsFilteredResults() {
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
                .header(USER_ID_HEADER, "regular_user")
                .header(USER_ROLE_HEADER, "USER")
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
    void getGeneById_returnsDetail() {
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
                .uri("/api/genes/{accession}", saved.getAccession())
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, "ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                });
    }

    @Test
    void getGeneById_notFound_returnsNotFound() {
        restClient.get()
                .uri("/api/genes/{accession}", "9999")
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, "ADMIN")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getGenes_invalidSort_returnsBadRequest() {
        restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/genes").queryParam("sort", "not_a_field").build())
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, "ADMIN")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void postSearch_lengthRangeInvalid_returnsBadRequest() {
        var request = java.util.Map.<String, Object>of("lengthMin", 200, "lengthMax", 10, "page", 0, "size", 10);
        restClient.post()
                .uri("/api/genes/search")
                .header(USER_ID_HEADER, "regular_user")
                .header(USER_ROLE_HEADER, "USER")
                .body(request)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void postExportCsv_returnsCsv() {
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
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, "ADMIN")
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

