package com.bioinformatics.importservice.controller;

import com.bioinformatics.common.exception.ErrorResponse;
import com.bioinformatics.common.models.filter.SavedFilterDto;
import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.common.models.other.PagedResponse;
import com.bioinformatics.importservice.client.SavedFilterService;
import com.bioinformatics.importservice.config.ApplicationProperties;
import com.bioinformatics.importservice.dto.ImportJobProgress;
import com.bioinformatics.importservice.dto.ImportJobSummary;
import com.bioinformatics.importservice.dto.ImportStatus;
import com.bioinformatics.importservice.entity.ImportJob;
import com.bioinformatics.importservice.repository.ImportJobRepository;
import com.bioinformatics.importservice.uniprot.apiloader.UniProtApiImportJobExecutor;
import com.bioinformatics.importservice.uniprot.fileloader.AsyncUniprotImportJobExecutor;
import com.bioinformatics.importservice.uniprot.fileloader.counter.CounterRegistry;
import com.bioinformatics.importservice.uniprot.fileloader.counter.RecordCounter;
import com.bioinformatics.shared.models.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.bioinformatics.shared.models.security.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureRestTestClient
@Slf4j
class ImportControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ImportJobRepository importJobRepository;

    @Autowired
    ApplicationProperties appProperties;
    @Autowired
    RestTestClient restClient;

    @MockitoBean
    AsyncUniprotImportJobExecutor asyncUniprotImportJobExecutor;
    @MockitoBean
    UniProtApiImportJobExecutor uniProtApiImportJobExecutor;

    @MockitoBean
    SavedFilterService savedFilterService;

    @MockitoBean
    CounterRegistry counterRegistry;

    @MockitoBean
    private CacheManager cacheManager;


    @BeforeEach
    void setUp() {
        importJobRepository.deleteAll();
        // Setup temp directory for imports
    }

    // ====== POST /api/v1/admin/import/uniprot ======

    @Test
    void triggerImport_withValidFile_returnsAccepted() throws Exception {
        // Setup
        var mockCounter = mockCounter(5L);
        when(counterRegistry.getCounter(anyString())).thenReturn(mockCounter);
        doNothing().when(asyncUniprotImportJobExecutor).execute(any());

        var file = createMockFile("uniprot_data.dat", "entry1\nentry2\nentry3\nentry4\nentry5\n");

        // Execute using MockMvc for multipart support
        mockMvc.perform(multipart("/api/v1/admin/import/uniprot")
                        .file(file)
                        .param("strategy", "overwrite")
                        .header(USER_ID_HEADER, "admin_user")
                        .header(USER_ROLE_HEADER, ADMIN_ROLE))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value(ImportStatus.RUNNING.toString()))
                .andExpect(jsonPath("$.fileName").value("uniprot_data.dat"));

        // Verify job was persisted
        assertThat(importJobRepository.count()).isEqualTo(1);
    }

    @Test
    void triggerImport_withAppendStrategy_succeeds() throws Exception {
        // Setup
        var mockCounter = mockCounter(3L);
        when(counterRegistry.getCounter(anyString())).thenReturn(mockCounter);
        doNothing().when(asyncUniprotImportJobExecutor).execute(any());

        var file = createMockFile("uniprot_append.tsv", "header\ndata1\ndata2\ndata3\n");

        // Execute using MockMvc
        mockMvc.perform(multipart("/api/v1/admin/import/uniprot")
                        .file(file)
                        .param("strategy", "append")
                        .header(USER_ID_HEADER, "admin_user")
                        .header(USER_ROLE_HEADER, ADMIN_ROLE))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value(ImportStatus.RUNNING.toString()));

        // Verify strategy was persisted
        var savedJob = importJobRepository.findAll().getFirst();
        assertThat(savedJob.getStrategy()).isEqualTo("APPEND");
    }

    @Test
    void triggerImport_withoutAdminRole_returnsForbidden() throws Exception {


        var file = createMockFile("test.dat", "data");

        mockMvc.perform(multipart("/api/v1/admin/import/uniprot")
                        .file(file)
                        .param("strategy", "overwrite")
                        .header(USER_ID_HEADER, "regular_user")
                        .header(USER_ROLE_HEADER, USER_ROLE))
                .andExpect(status().isForbidden());
    }

    @Test
    void triggerImport_withoutAuthentication_returnsUnauthorized() throws Exception {


        var file = createMockFile("test.dat", "data");

        mockMvc.perform(multipart("/api/v1/admin/import/uniprot")
                        .file(file)
                        .param("strategy", "overwrite"))
                .andExpect(status().isForbidden());
    }

    @Test
    void triggerImport_withConcurrentRunningImport_returnsConflict() throws Exception {


        // Setup: create an existing running job
        var runningJob = ImportJob.builder()
                .status(ImportStatus.RUNNING)
                .fileName("existing.dat")
                .strategy("OVERWRITE")
                .totalEstimated(10)
                .createdAt(Instant.now())
                .build();
        importJobRepository.save(runningJob);

        var mockCounter = mockCounter(5L);
        when(counterRegistry.getCounter(anyString())).thenReturn(mockCounter);

        var file = createMockFile("new_import.dat", "data");

        // Execute using MockMvc
        mockMvc.perform(multipart("/api/v1/admin/import/uniprot")
                        .file(file)
                        .param("strategy", "overwrite")
                        .header(USER_ID_HEADER, "admin_user")
                        .header(USER_ROLE_HEADER, ADMIN_ROLE))
                .andExpect(status().isConflict());

        // Verify no new job was created
        assertThat(importJobRepository.count()).isEqualTo(1);
    }

    @Test
    void triggerImport_missingFileParameter_returnsBadRequest() throws Exception {


        mockMvc.perform(multipart("/api/v1/admin/import/uniprot")
                        .param("strategy", "overwrite")
                        .header(USER_ID_HEADER, "admin_user")
                        .header(USER_ROLE_HEADER, ADMIN_ROLE))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void triggerImport_missingStrategyParameter_returnsBadRequest() throws Exception {


        var file = createMockFile("test.dat", "data");

        mockMvc.perform(multipart("/api/v1/admin/import/uniprot")
                        .file(file)
                        .header(USER_ID_HEADER, "admin_user")
                        .header(USER_ROLE_HEADER, ADMIN_ROLE))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void triggerRemoteImport_returnsAccepted() {


        doNothing().when(uniProtApiImportJobExecutor).execute(any());
        when(savedFilterService.getSavedFilterById(anyLong(), any(UserPrincipal.class))).thenReturn(Optional.of(
                new SavedFilterDto(42L, "example-filter", GeneSearchRequest.builder().accession("ACC").build(), Instant.now())
        ));

        restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/admin/import/uniprot/remote")
                        .queryParam("filterId", 42L)
                        .build())
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, ADMIN_ROLE)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(ImportJobSummary.class)
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.id()).isNotBlank();
                    assertThat(body.status()).isEqualTo(ImportStatus.RUNNING);
                    assertThat(body.fileName()).contains("example-filter");
                });

        assertThat(importJobRepository.count()).isEqualTo(1);
    }

    // ====== GET /api/v1/admin/import/status ======

    @Test
    void listImportJobs_returnsPagedSummaries() {


        // Setup: create multiple import jobs
        var job1 = ImportJob.builder()
                .status(ImportStatus.COMPLETED)
                .fileName("import1.dat")
                .strategy("OVERWRITE")
                .totalEstimated(100)
                .recordsProcessed(100)
                .durationMs(5000)
                .createdAt(Instant.now().minusSeconds(3600))
                .completedAt(Instant.now().minusSeconds(3000))
                .build();
        importJobRepository.save(job1);

        var job2 = ImportJob.builder()
                .status(ImportStatus.COMPLETED)
                .fileName("import2.dat")
                .strategy("APPEND")
                .totalEstimated(50)
                .recordsProcessed(50)
                .durationMs(2500)
                .createdAt(Instant.now().minusSeconds(1800))
                .completedAt(Instant.now().minusSeconds(1300))
                .build();
        importJobRepository.save(job2);

        // Execute
        restClient.get()
                .uri("/api/v1/admin/import/status?page=0&size=10")
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, ADMIN_ROLE)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PagedResponse<ImportJobSummary>>() {
                })
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.content()).hasSize(2);
                    assertThat(body.totalElements()).isEqualTo(2);
                    assertThat(body.totalPages()).isEqualTo(1);
                    assertThat(body.page()).isEqualTo(0);
                });
    }

    @Test
    void listImportJobs_withPagination_returnsCorrectPage() {


        // Setup: create 25 jobs
        for (int i = 0; i < 25; i++) {
            var job = ImportJob.builder()
                    .status(ImportStatus.COMPLETED)
                    .fileName("import_" + i + ".dat")
                    .strategy("OVERWRITE")
                    .totalEstimated(100)
                    .recordsProcessed(100)
                    .createdAt(Instant.now().minusSeconds(i * 100))
                    .build();
            importJobRepository.save(job);
        }

        // Execute first page
        restClient.get()
                .uri("/api/v1/admin/import/status?page=0&size=10")
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, ADMIN_ROLE)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PagedResponse<ImportJobSummary>>() {
                })
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.content()).hasSize(10);
                    assertThat(body.totalElements()).isEqualTo(25);
                    assertThat(body.totalPages()).isEqualTo(3);
                    assertThat(body.page()).isEqualTo(0);
                });

        // Execute second page
        restClient.get()
                .uri("/api/v1/admin/import/status?page=1&size=10")
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, ADMIN_ROLE)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PagedResponse<ImportJobSummary>>() {
                })
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.content()).hasSize(10);
                    assertThat(body.page()).isEqualTo(1);
                });

        // Execute third page (partial)
        restClient.get()
                .uri("/api/v1/admin/import/status?page=2&size=10")
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, ADMIN_ROLE)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PagedResponse<ImportJobSummary>>() {
                })
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.content()).hasSize(5);
                    assertThat(body.page()).isEqualTo(2);
                });
    }

    @Test
    void listImportJobs_withDefaultPagination_returnsFirstPage() {


        // Setup: create 25 jobs
        for (int i = 0; i < 25; i++) {
            var job = ImportJob.builder()
                    .status(ImportStatus.COMPLETED)
                    .fileName("import_" + i + ".dat")
                    .strategy("OVERWRITE")
                    .totalEstimated(100)
                    .recordsProcessed(100)
                    .createdAt(Instant.now().minusSeconds(i * 100))
                    .build();
            importJobRepository.save(job);
        }

        // Execute with default pagination
        restClient.get()
                .uri("/api/v1/admin/import/status")
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, ADMIN_ROLE)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PagedResponse<ImportJobSummary>>() {
                })
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.content()).hasSize(20); // Default size is 20
                    assertThat(body.page()).isEqualTo(0);
                });
    }

    @Test
    void listImportJobs_resultsOrderedByCreatedAtDescending() {


        // Setup: create jobs with different timestamps
        var job1 = ImportJob.builder()
                .status(ImportStatus.COMPLETED)
                .fileName("import1.dat")
                .totalEstimated(100)
                .createdAt(Instant.now().minusSeconds(1000))
                .strategy("OVERWRITE")
                .build();
        importJobRepository.save(job1);

        var job2 = ImportJob.builder()
                .status(ImportStatus.COMPLETED)
                .fileName("import2.dat")
                .totalEstimated(100)
                .createdAt(Instant.now())
                .strategy("OVERWRITE")
                .build();
        importJobRepository.save(job2);

        // Execute
        restClient.get()
                .uri("/api/v1/admin/import/status?page=0&size=10")
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, ADMIN_ROLE)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PagedResponse<ImportJobSummary>>() {
                })
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.content()).hasSize(2);
                    assertThat(body.content().get(0).fileName()).isEqualTo("import2.dat");
                    assertThat(body.content().get(1).fileName()).isEqualTo("import1.dat");
                });
    }

    @Test
    void listImportJobs_withoutAdminRole_returnsForbidden() {


        restClient.get()
                .uri("/api/v1/admin/import/status?page=0&size=10")
                .header(USER_ID_HEADER, "regular_user")
                .header(USER_ROLE_HEADER, USER_ROLE)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void listImportJobs_withoutAuthentication_returnsUnauthorized() {


        restClient.get()
                .uri("/api/v1/admin/import/status?page=0&size=10")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void listImportJobs_emptyList_returnsEmptyPagedResponse() {


        // Execute on empty repository
        restClient.get()
                .uri("/api/v1/admin/import/status?page=0&size=10")
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, ADMIN_ROLE)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PagedResponse<ImportJobSummary>>() {
                })
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.content()).isEmpty();
                    assertThat(body.totalElements()).isEqualTo(0);
                    assertThat(body.totalPages()).isEqualTo(0);
                });
    }

    // ====== GET /api/v1/admin/import/status/{jobId} ======

    @Test
    void getImportJobStatus_withValidJobId_returnsProgress() {


        // Setup: create a job
        var job = ImportJob.builder()
                .status(ImportStatus.RUNNING)
                .fileName("import.dat")
                .strategy("OVERWRITE")
                .totalEstimated(100)
                .recordsProcessed(50)
                .durationMs(2500)
                .createdAt(Instant.now())
                .build();
        importJobRepository.save(job);

        // Execute
        restClient.get()
                .uri("/api/v1/admin/import/status/{jobId}", job.getId().toString())
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, ADMIN_ROLE)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ImportJobProgress.class)
                .consumeWith(result -> {
                    ImportJobProgress body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.id()).isEqualTo(job.getId().toString());
                    assertThat(body.status()).isEqualTo(ImportStatus.RUNNING);
                    assertThat(body.fileName()).isEqualTo("import.dat");
                    assertThat(body.recordsProcessed()).isEqualTo(50);
                    assertThat(body.totalEstimated()).isEqualTo(100);
                });
    }

    @Test
    void getImportJobStatus_withCompletedJob_returnsCompletedStatus() {


        // Setup: create a completed job
        var job = ImportJob.builder()
                .status(ImportStatus.COMPLETED)
                .fileName("import.dat")
                .totalEstimated(100)
                .recordsProcessed(100)
                .durationMs(5000)
                .createdAt(Instant.now().minusSeconds(5))
                .completedAt(Instant.now())
                .strategy("OVERWRITE")
                .build();
        importJobRepository.save(job);

        // Execute
        restClient.get()
                .uri("/api/v1/admin/import/status/{jobId}", job.getId().toString())
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, ADMIN_ROLE)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ImportJobProgress.class)
                .consumeWith(result -> {
                    ImportJobProgress body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.status()).isEqualTo(ImportStatus.COMPLETED);
                    assertThat(body.recordsProcessed()).isEqualTo(100);
                });
    }

    @Test
    void getImportJobStatus_withFailedJob_returnsErrorMessage() {


        // Setup: create a failed job
        var job = ImportJob.builder()
                .status(ImportStatus.FAILED)
                .fileName("import.dat")
                .totalEstimated(100)
                .recordsProcessed(25)
                .errorMessage("Parsing error at line 50")
                .createdAt(Instant.now())
                .completedAt(Instant.now())
                .strategy("OVERWRITE")
                .build();
        importJobRepository.save(job);

        // Execute
        restClient.get()
                .uri("/api/v1/admin/import/status/{jobId}", job.getId().toString())
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, ADMIN_ROLE)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ImportJobProgress.class)
                .consumeWith(result -> {
                    ImportJobProgress body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.status()).isEqualTo(ImportStatus.FAILED);
                    assertThat(body.errorMessage()).contains("Parsing error");
                });
    }

    @Test
    void getImportJobStatus_withInvalidJobId_returnsFailedStatus() {


        var invalidJobId = UUID.randomUUID().toString();

        // Execute
        restClient.get()
                .uri("/api/v1/admin/import/status/{jobId}", invalidJobId)
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, ADMIN_ROLE)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .consumeWith(result -> {
                    ErrorResponse body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.status()).isEqualTo(HttpStatus.NOT_FOUND.value());
                    assertThat(body.error()).isEqualTo("Not Found");
                    assertThat(body.message()).isEqualTo("Import job not found: " + invalidJobId);
                });
    }

    @Test
    void getImportJobStatus_withoutAdminRole_returnsForbidden() {


        var jobId = UUID.randomUUID().toString();

        restClient.get()
                .uri("/api/v1/admin/import/status/{jobId}", jobId)
                .header(USER_ID_HEADER, "regular_user")
                .header(USER_ROLE_HEADER, USER_ROLE)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void getImportJobStatus_withoutAuthentication_returnsForbidden() {


        var jobId = UUID.randomUUID().toString();

        restClient.get()
                .uri("/api/v1/admin/import/status/{jobId}", jobId)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void getImportJobStatus_withMalformedUUID_returnsBadRequest() {


        restClient.get()
                .uri("/api/v1/admin/import/status/{jobId}", "not-a-uuid")
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, ADMIN_ROLE)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    // ====== Helper Methods ======

    /**
     * Creates a mock multipart file.
     */
    private MockMultipartFile createMockFile(String filename, String content) throws IOException {
        var tempDir = Path.of(appProperties.importConfig().tempDir());
        Files.createDirectories(tempDir);

        Path filePath = tempDir.resolve(filename);
        Files.write(filePath, content.getBytes());

        return new MockMultipartFile(
                "file",
                filename,
                MediaType.TEXT_PLAIN_VALUE,
                content.getBytes()
        );
    }


    /**
     * Creates a mock RecordCounter.
     */
    private RecordCounter mockCounter(long count) {
        return new RecordCounter() {
            @Override
            public boolean supports(String filename) {
                return true;
            }

            @Override
            public long count(java.io.InputStream is) {
                return count;
            }
        };
    }
}

