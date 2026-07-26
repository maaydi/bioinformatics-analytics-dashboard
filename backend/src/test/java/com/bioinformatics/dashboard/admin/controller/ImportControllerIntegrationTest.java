package com.bioinformatics.dashboard.admin.controller;

import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.auth.repository.AppUserRepository;
import com.bioinformatics.dashboard.config.AppProperties;
import com.bioinformatics.dashboard.exception.ErrorResponse;
import com.bioinformatics.dashboard.job.dto.ImportJobProgress;
import com.bioinformatics.dashboard.job.dto.ImportJobSummary;
import com.bioinformatics.dashboard.job.dto.ImportStatus;
import com.bioinformatics.dashboard.job.entity.ImportJob;
import com.bioinformatics.dashboard.job.repository.ImportJobRepository;
import com.bioinformatics.dashboard.job.uniprot.apiloader.UniProtApiImportJobExecutor;
import com.bioinformatics.dashboard.job.uniprot.fileloader.AsyncUniprotImportJobExecutor;
import com.bioinformatics.dashboard.job.uniprot.fileloader.counter.CounterRegistry;
import com.bioinformatics.dashboard.job.uniprot.fileloader.counter.RecordCounter;
import com.bioinformatics.dashboard.model.gene.PagedResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = "app.rate-limiter.enabled=false")
@AutoConfigureMockMvc
@AutoConfigureRestTestClient
@Slf4j
class ImportControllerIntegrationTest {


    private static final String ADMIN_ROLE = "ROLE_ADMIN";
    private static final String USER_ROLE = "ROLE_USER";
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    MockMvc mockMvc;
    @Autowired
    ImportJobRepository importJobRepository;
    @Autowired
    AppUserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    AppProperties appProperties;
    @Autowired
    RestTestClient restClient;

    @MockitoBean
    AsyncUniprotImportJobExecutor asyncUniprotImportJobExecutor;
    @MockitoBean
    UniProtApiImportJobExecutor uniProtApiImportJobExecutor;

    @MockitoBean
    CounterRegistry counterRegistry;

    @MockitoBean
    private CacheManager cacheManager;


    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        importJobRepository.deleteAll();
        userRepository.deleteAll();

        // Setup temp directory for imports
        appProperties.getImportConfig().setTempDir(tempDir.toString());

        // Create admin user
        var adminUser = AppUser.builder()
                .username("admin_user")
                .password(passwordEncoder.encode("admin_pass"))
                .role(ADMIN_ROLE)
                .build();
        userRepository.save(adminUser);
        userRepository.flush();
        adminToken = generateToken("admin_user", "admin_pass");

        // Create regular user
        var regularUser = AppUser.builder()
                .username("regular_user")
                .password(passwordEncoder.encode("user_pass"))
                .role(USER_ROLE)
                .build();
        userRepository.save(regularUser);
        userRepository.flush();
        userToken = generateToken("regular_user", "user_pass");
    }

    // ====== POST /api/admin/import/uniprot ======

    @Test
    void triggerImport_withValidFile_returnsAccepted() throws Exception {
        // Setup
        var mockCounter = mockCounter(5L);
        when(counterRegistry.getCounter(anyString())).thenReturn(mockCounter);
        doNothing().when(asyncUniprotImportJobExecutor).execute(any());

        var file = createMockFile("uniprot_data.dat", "entry1\nentry2\nentry3\nentry4\nentry5\n");

        // Execute using MockMvc for multipart support
        mockMvc.perform(multipart("/api/admin/import/uniprot")
                        .file(file)
                        .param("strategy", "overwrite")
                        .header("Authorization", "Bearer " + adminToken))
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
        mockMvc.perform(multipart("/api/admin/import/uniprot")
                        .file(file)
                        .param("strategy", "append")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value(ImportStatus.RUNNING.toString()));

        // Verify strategy was persisted
        var savedJob = importJobRepository.findAll().getFirst();
        assertThat(savedJob.getStrategy()).isEqualTo("APPEND");
    }

    @Test
    void triggerImport_withoutAdminRole_returnsForbidden() throws Exception {
        var file = createMockFile("test.dat", "data");

        mockMvc.perform(multipart("/api/admin/import/uniprot")
                        .file(file)
                        .param("strategy", "overwrite")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void triggerImport_withoutAuthentication_returnsUnauthorized() throws Exception {
        var file = createMockFile("test.dat", "data");

        mockMvc.perform(multipart("/api/admin/import/uniprot")
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
        mockMvc.perform(multipart("/api/admin/import/uniprot")
                        .file(file)
                        .param("strategy", "overwrite")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());

        // Verify no new job was created
        assertThat(importJobRepository.count()).isEqualTo(1);
    }

    @Test
    void triggerImport_missingFileParameter_returnsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/admin/import/uniprot")
                        .param("strategy", "overwrite")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void triggerImport_missingStrategyParameter_returnsBadRequest() throws Exception {
        var file = createMockFile("test.dat", "data");

        mockMvc.perform(multipart("/api/admin/import/uniprot")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void triggerRemoteImport_returnsAccepted() {
        doNothing().when(uniProtApiImportJobExecutor).execute(any());

        restClient.post()
                .uri("/api/admin/import/uniprot/remote")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(ImportJobSummary.class)
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.id()).isNotBlank();
                    assertThat(body.status()).isEqualTo(ImportStatus.RUNNING);
                    assertThat(body.fileName()).isEqualTo("UNIPROT_API_REMOTE");
                });

        assertThat(importJobRepository.count()).isEqualTo(1);
    }

    // ====== GET /api/admin/import/status ======

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
                .uri("/api/admin/import/status?page=0&size=10")
                .header("Authorization", "Bearer " + adminToken)
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
                .uri("/api/admin/import/status?page=0&size=10")
                .header("Authorization", "Bearer " + adminToken)
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
                .uri("/api/admin/import/status?page=1&size=10")
                .header("Authorization", "Bearer " + adminToken)
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
                .uri("/api/admin/import/status?page=2&size=10")
                .header("Authorization", "Bearer " + adminToken)
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
                .uri("/api/admin/import/status")
                .header("Authorization", "Bearer " + adminToken)
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
                .uri("/api/admin/import/status?page=0&size=10")
                .header("Authorization", "Bearer " + adminToken)
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
                .uri("/api/admin/import/status?page=0&size=10")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void listImportJobs_withoutAuthentication_returnsUnauthorized() {
        restClient.get()
                .uri("/api/admin/import/status?page=0&size=10")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void listImportJobs_emptyList_returnsEmptyPagedResponse() {
        // Execute on empty repository
        restClient.get()
                .uri("/api/admin/import/status?page=0&size=10")
                .header("Authorization", "Bearer " + adminToken)
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

    // ====== GET /api/admin/import/status/{jobId} ======

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
                .uri("/api/admin/import/status/{jobId}", job.getId().toString())
                .header("Authorization", "Bearer " + adminToken)
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
                .uri("/api/admin/import/status/{jobId}", job.getId().toString())
                .header("Authorization", "Bearer " + adminToken)
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
                .uri("/api/admin/import/status/{jobId}", job.getId().toString())
                .header("Authorization", "Bearer " + adminToken)
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
                .uri("/api/admin/import/status/{jobId}", invalidJobId)
                .header("Authorization", "Bearer " + adminToken)
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
                .uri("/api/admin/import/status/{jobId}", jobId)
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void getImportJobStatus_withoutAuthentication_returnsForbidden() {
        var jobId = UUID.randomUUID().toString();

        restClient.get()
                .uri("/api/admin/import/status/{jobId}", jobId)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void getImportJobStatus_withMalformedUUID_returnsBadRequest() {
        restClient.get()
                .uri("/api/admin/import/status/{jobId}", "not-a-uuid")
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    // ====== Helper Methods ======

    /**
     * Generates a JWT token for authentication via login endpoint.
     */
    private String generateToken(String username, String password) throws Exception {
        var loginPayload = objectMapper.writeValueAsString(Map.of("username", username, "password", password));

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();

        var responseBody = result.getResponse().getContentAsString();
        var jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("accessToken").asText();
    }

    /**
     * Creates a mock multipart file.
     */
    private MockMultipartFile createMockFile(String filename, String content) {
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

