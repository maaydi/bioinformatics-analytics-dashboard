package com.bioinformatics.exportservice.controller;

import com.bioinformatics.common.models.PagedResponse;
import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.exportservice.batch.ExportJobExecutor;
import com.bioinformatics.exportservice.dto.*;
import com.bioinformatics.exportservice.entity.ExportPipeline;
import com.bioinformatics.exportservice.repository.ExportPipelineRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Instant;
import java.util.List;

import static com.bioinformatics.shared.models.security.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ExportPipelineControllerIntegrationTest {

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private ExportPipelineRepository exportPipelineRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExportJobExecutor exportJobExecutor;

    @BeforeEach
    void setUp() {
        exportPipelineRepository.deleteAll();
    }

    @Test
    void createPipeline_withValidRequest_returnsCreated() {
        var request = new ExportPipelineCreateRequest(
                "protein-export",
                "Export accession and names",
                GeneSearchRequest.builder()
                        .accession("P12345")
                        .reviewed(true)
                        .page(0)
                        .size(10)
                        .direction("asc")
                        .sort("accession")
                        .build(),
                DefaultExportFormat.CSV,
                List.of("accession", "entryName", "geneNamePrimary")
        );

        restClient.post()
                .uri("/api/v1/exports/pipelines")
                .contentType(MediaType.APPLICATION_JSON)
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, ADMIN_ROLE)
                .header(DATA_PROVIDER_HEADER, POSTGRES_DATA_PROVIDER)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ExportPipelineResponse.class)
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.name()).isEqualTo("protein-export");
                    assertThat(body.status()).isEqualTo(ExportStatus.QUEUED);
                    assertThat(body.format()).isEqualTo(DefaultExportFormat.CSV);
                    assertThat(body.fieldSchema()).containsExactly("accession", "entryName", "geneNamePrimary");
                });

        assertThat(exportPipelineRepository.count()).isEqualTo(1);
    }

    @Test
    void listPipelines_returnsPagedSummaries() {
        exportPipelineRepository.save(buildPipeline("export-one", ExportStatus.COMPLETED));
        exportPipelineRepository.save(buildPipeline("export-two", ExportStatus.RUNNING));

        restClient.get()
                .uri("/api/v1/exports/pipelines?page=0&size=10")
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, ADMIN_ROLE)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PagedResponse<ExportPipelineResponse>>() {
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
    void getPipeline_withValidId_returnsPipeline() {
        var saved = exportPipelineRepository.save(buildPipeline("detail-export", ExportStatus.QUEUED));

        restClient.get()
                .uri("/api/v1/exports/pipelines/{id}", saved.getId())
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, ADMIN_ROLE)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ExportPipelineResponse.class)
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.id()).isEqualTo(saved.getId());
                    assertThat(body.name()).isEqualTo("detail-export");
                    assertThat(body.status()).isEqualTo(ExportStatus.QUEUED);
                    assertThat(body.filter().accession()).isEqualTo("P12345");
                });
    }

    @Test
    void getPipelineStatus_withValidId_returnsProgress() {
        var saved = exportPipelineRepository.save(buildPipeline("status-export", ExportStatus.RUNNING));

        restClient.get()
                .uri("/api/v1/exports/pipelines/{id}/status", saved.getId())
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, ADMIN_ROLE)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ExportJobStatusResponse.class)
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body.pipelineId()).isEqualTo(saved.getId());
                    assertThat(body.status()).isEqualTo(ExportStatus.RUNNING);
                    assertThat(body.progressPercent()).isZero();
                    assertThat(body.updatedAt()).isNotNull();
                });
    }

    @Test
    void getAvailableFields_returnsFieldSchemas() {
        restClient.get()
                .uri("/api/v1/exports/fields")
                .header(USER_ID_HEADER, "admin_user")
                .header(USER_ROLE_HEADER, ADMIN_ROLE)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(result -> {
                    var body = result.getResponseBody();
                    assertThat(body).isNotNull();
                    assertThat(body).isNotEmpty();
                    assertThat(new String(body, java.nio.charset.StandardCharsets.UTF_8)).contains("fieldName");
                });
    }

    @Test
    void listPipelines_withoutAuthentication_returnsForbidden() {
        restClient.get()
                .uri("/api/v1/exports/pipelines?page=0&size=10")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void createPipeline_withoutAllowedRole_returnsForbidden() {
        var request = new ExportPipelineCreateRequest(
                "blocked-export",
                "blocked",
                GeneSearchRequest.builder().accession("P12345").build(),
                DefaultExportFormat.CSV,
                List.of("accession")
        );

        restClient.post()
                .uri("/api/v1/exports/pipelines")
                .contentType(MediaType.APPLICATION_JSON)
                .header(USER_ID_HEADER, "guest_user")
                .header(USER_ROLE_HEADER, "GUEST")
                .body(request)
                .exchange()
                .expectStatus().isForbidden();
    }

    private ExportPipeline buildPipeline(String name, ExportStatus status) {
        return ExportPipeline.builder()
                .userId("admin_user")
                .name(name)
                .description("Generated by test")
                .filterJson(GeneSearchRequest.builder()
                        .accession("P12345")
                        .reviewed(true)
                        .page(0)
                        .size(10)
                        .direction("asc")
                        .sort("accession")
                        .build())
                .format(DefaultExportFormat.CSV)
                .fieldSchema(objectMapper.valueToTree(List.of("accession", "entryName", "geneNamePrimary")))
                .status(status)
                .createdAt(Instant.now())
                .build();
    }
}

