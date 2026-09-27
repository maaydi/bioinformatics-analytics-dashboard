package com.bioinformatics.exportservice.service;

import com.bioinformatics.common.exception.AccessDeniedException;
import com.bioinformatics.common.exception.ConflictException;
import com.bioinformatics.common.exception.ResourceDeletedException;
import com.bioinformatics.common.exception.ResourceNotFoundException;
import com.bioinformatics.common.gene.dto.ProteinDetailDto;
import com.bioinformatics.common.models.PagedResponse;
import com.bioinformatics.common.providers.DataProvider;
import com.bioinformatics.exportservice.batch.ExportJobExecutor;
import com.bioinformatics.exportservice.dto.*;
import com.bioinformatics.exportservice.entity.ExportPipeline;
import com.bioinformatics.exportservice.mapper.ExportPipelineMapper;
import com.bioinformatics.exportservice.repository.ExportJobExecutionRepository;
import com.bioinformatics.exportservice.repository.ExportPipelineRepository;
import com.bioinformatics.exportservice.writer.ExportWriterFactory;
import com.bioinformatics.shared.models.gene.ExportFieldSchema;
import com.bioinformatics.shared.models.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;

/**
 * Service for managing export pipeline lifecycle and operations.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Pipeline creation and initialization
 *   <li>Async job execution via Spring Batch
 *   <li>Status tracking and progress monitoring
 *   <li>File download management
 *   <li>Pipeline retry and deletion (soft-delete)
 * </ul>
 *
 * <p>Tracks execution flow:
 * {@code createPipeline} → {@code executePipelineJob} → {@code ExportJobExecutor}
 * → Batch Job Listeners → {@code ExportPipelineLifeCycleService} (status updates)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExportPipelineService {

    private final ExportPipelineRepository pipelineRepository;
    private final ExportJobExecutionRepository jobExecutionRepository;
    private final ExportPipelineMapper mapper;
    private final ExportJobExecutor executor;
    private final JobRepository jobRepository;
    private final ExportWriterFactory writerFactory;
    private final ExportPipelineLifeCycleService taskletService;


    /**
     * Creates a new export pipeline and enqueues it for async processing.
     *
     * <p>Workflow:
     * <ol>
     *   <li>Maps DTO to entity with user ID
     *   <li>Persists pipeline (status: QUEUED)
     *   <li>Enqueues Spring Batch job
     * </ol>
     *
     * @param request   export configuration (filter, format, fields)
     * @param initiator user requesting the export
     * @return created pipeline DTO
     */
    public ExportPipelineResponse createPipeline(ExportPipelineCreateRequest request, UserPrincipal initiator) {
        log.info("[EXPORT] Creating pipeline: name='{}', description='{}', user='{}', format='{}'",
                request.name(), request.description(), initiator.id(), request.format());

        var pipeline = mapper.toEntity(request, initiator.id());
        log.debug("[EXPORT] Pipeline entity mapped - ID={} (not yet persisted)", pipeline.getId());

        var result = pipelineRepository.save(pipeline);
        log.info("[EXPORT] Pipeline persisted to database - ID={}, status=QUEUED, createdAt={}",
                result.getId(), result.getCreatedAt());

        executePipelineJob(result, initiator, request.fieldSchema());
        log.info("[EXPORT] Pipeline job enqueued for async execution - ID={}", result.getId());

        return mapper.toDto(result);
    }

    /**
     * Lists pipelines for a user with optional status filtering.
     *
     * @param status   optional status filter (null means all statuses)
     * @param pageable pagination parameters
     * @param user     authenticated user
     * @return paginated list of pipelines
     */
    public PagedResponse<ExportPipelineResponse> listPipelines(ExportStatus status, Pageable pageable, UserPrincipal user) {
        log.debug("[EXPORT] Listing pipelines - user='{}', status={}, page={}, size={}",
                user.id(), status, pageable.getPageNumber(), pageable.getPageSize());

        var page = (status != null)
                ? pipelineRepository.findByUserIdAndStatusAndDeletedAtIsNull(user.id(), status, pageable)
                : pipelineRepository.findByUserIdAndDeletedAtIsNull(user.id(), pageable);

        log.debug("[EXPORT] Found {} pipelines for user '{}'", page.getTotalElements(), user.id());
        return PagedResponse.of(page.map(mapper::toDto));
    }

    /**
     * Retrieves a single pipeline by ID, verifying ownership.
     *
     * @param pipelineId pipeline identifier
     * @param user       authenticated user
     * @return pipeline DTO
     * @throws ResourceNotFoundException if not found
     * @throws ResourceDeletedException  if soft-deleted
     * @throws AccessDeniedException     if not owned by user
     */
    public ExportPipelineResponse getPipeline(Long pipelineId, UserPrincipal user) {
        log.debug("[EXPORT] Fetching pipeline - ID={}, user='{}'", pipelineId, user.id());
        var pipeline = findNotDeletedPipelineByIdAndOwner(pipelineId, user);
        log.debug("[EXPORT] Pipeline retrieved - ID={}, status={}", pipeline.getId(), pipeline.getStatus());
        return mapper.toDto(pipeline);
    }

    /**
     * Polls current execution status of a pipeline.
     *
     * <p>Returns:
     * <ul>
     *   <li>Current status (QUEUED/RUNNING/COMPLETED/FAILED)
     *   <li>Progress percentage (0–100)
     *   <li>Chunk metrics (processed/total)
     *   <li>Current step name from batch job
     *   <li>Last update timestamp
     * </ul>
     *
     * @param pipelineId pipeline identifier
     * @param user       authenticated user
     * @return detailed status response
     */
    public ExportJobStatusResponse getPipelineStatus(Long pipelineId, UserPrincipal user) {
        log.debug("[EXPORT] Polling status - ID={}, user='{}'", pipelineId, user.id());

        var pipeline = findNotDeletedPipelineByIdAndOwner(pipelineId, user);
        log.debug("[EXPORT] Pipeline status check - ID={}, current_status={}", pipeline.getId(), pipeline.getStatus());

        var execution = jobExecutionRepository.findByPipelineId(pipelineId);
        if (execution.isPresent()) {
            log.debug("[EXPORT] Job execution found - ID={}, chunks_processed={}/{}, progress={}%",
                    execution.get().getId(), execution.get().getChunksProcessed(),
                    execution.get().getChunksTotal(), execution.get().getProgressPercent());

            var currentStep = ExportJobStepTracker.determineCurrentStep(jobRepository.getJobExecution(pipeline.getJobExecutionId()));
            log.debug("[EXPORT] Current step - ID={}, step_name={}", pipelineId, currentStep);

            return new ExportJobStatusResponse(pipelineId,
                    pipeline.getStatus(),
                    execution.get().getProgressPercent(),
                    execution.get().getChunksProcessed(),
                    execution.get().getChunksTotal(),
                    currentStep,
                    execution.get().getUpdatedAt()
            );
        } else {
            log.debug("[EXPORT] No execution record found - ID={}, likely QUEUED status", pipelineId);
            return new ExportJobStatusResponse(pipelineId, pipeline.getStatus(), 0, 0, 0, null, Instant.now());
        }
    }

    /**
     * Generates a download URL for a completed export.
     *
     * @param pipelineId pipeline identifier
     * @param user       authenticated user
     * @return download URL and file metadata
     * @throws NoSuchFileException if pipeline not completed or file missing
     */
    public DownloadUrlDto getDownloadUrl(Long pipelineId, UserPrincipal user) throws IOException {
        log.debug("[EXPORT] Requesting download URL - ID={}, user='{}'", pipelineId, user.id());

        var pipeline = findNotDeletedPipelineByIdAndOwner(pipelineId, user);
        if (pipeline.isCompleted()) {
            log.debug("[EXPORT] Download ready - ID={}, fileName='{}', size={} bytes",
                    pipelineId, pipeline.getFileName(), pipeline.getFileSizeBytes());
            return new DownloadUrlDto("/exports/pipelines/%d/download-file".formatted(pipelineId),
                    pipeline.getFileName(), pipeline.getFileSizeBytes(), pipeline.getFormat().getContentType());
        }

        var errorMsg = pipeline.isTerminal()
                ? "does not complete properly"
                : "is still running";
        log.warn("[EXPORT] Download unavailable - ID={}, reason: {}", pipelineId, errorMsg);
        throw new NoSuchFileException("Export pipeline %s [%d] %s".formatted(pipeline.getName(), pipeline.getId(), errorMsg));
    }

    /**
     * Retries a failed or cancelled export by cloning the configuration.
     *
     * @param pipelineId failed pipeline identifier
     * @param user       authenticated user
     * @return new pipeline response
     * @throws ConflictException if pipeline still running
     */
    public ExportPipelineResponse retryPipeline(Long pipelineId, UserPrincipal user) {
        log.info("[EXPORT] Retry requested - ID={}, user='{}'", pipelineId, user.id());

        var pipeline = findNotDeletedPipelineByIdAndOwner(pipelineId, user);
        if (!pipeline.isTerminal()) {
            log.warn("[EXPORT] Cannot retry running job - ID={}", pipelineId);
            throw new ConflictException("Export pipeline job %s [%d] is still running.".formatted(pipeline.getName(), pipelineId));
        }

        log.debug("[EXPORT] Cloning pipeline config for retry - original_ID={}", pipelineId);
        var dto = mapper.toDto(pipeline);
        var clonedPipeline = new ExportPipelineCreateRequest(dto.name(), dto.description(), dto.filter(), dto.format(), dto.fieldSchema());
        var newPipeline = createPipeline(clonedPipeline, user);

        log.info("[EXPORT] Retry pipeline created - original_ID={}, new_ID={}", pipelineId, newPipeline.id());
        return newPipeline;
    }

    /**
     * Soft-deletes a pipeline (sets deletedAt timestamp).
     *
     * <p>If still running, stops the batch job before deletion.
     *
     * @param pipelineId pipeline identifier
     * @param user       authenticated user
     */
    public void deletePipeline(Long pipelineId, UserPrincipal user) {
        log.info("[EXPORT] Delete requested - ID={}, user='{}'", pipelineId, user.id());

        var pipeline = findNotDeletedPipelineByIdAndOwner(pipelineId, user);
        if (!pipeline.isTerminal()) {
            try {
                var execution = jobRepository.getJobExecution(pipeline.getJobExecutionId());
                if (execution != null) {
                    log.info("[EXPORT] Stopping running job before delete - ID={}, execution_ID={}",
                            pipelineId, pipeline.getJobExecutionId());
                    executor.stop(execution);
                } else {
                    log.warn("[EXPORT] Job execution not found, cannot stop - ID={}", pipelineId);
                }
            } catch (Exception e) {
                log.debug("[EXPORT] Exception stopping job (expected if not running) - ID={}, error={}",
                        pipelineId, e.getMessage());
            }
        }

        pipeline.setDeletedAt(Instant.now());
        log.info("[EXPORT] Pipeline marked as deleted - ID={}, deletedAt={}", pipelineId, pipeline.getDeletedAt());
    }

    /**
     * Opens an InputStream for a completed export file.
     *
     * @param pipelineId pipeline identifier
     * @param user       authenticated user
     * @return file stream with metadata
     * @throws NoSuchFileException if pipeline not completed or file missing
     */
    public ExportFileStream getExportFileStream(Long pipelineId, UserPrincipal user) throws IOException {
        log.debug("[EXPORT] Opening file stream - ID={}, user='{}'", pipelineId, user.id());

        var pipeline = findNotDeletedPipelineByIdAndOwner(pipelineId, user);
        if (pipeline.isCompleted()) {
            var file = Paths.get(pipeline.getFilePath());
            if (Files.exists(file)) {
                log.debug("[EXPORT] File exists and ready for download - ID={}, path='{}', size={} bytes",
                        pipelineId, pipeline.getFilePath(), pipeline.getFileSizeBytes());
                var steam = Files.newInputStream(file, StandardOpenOption.READ);
                return new ExportFileStream(
                        steam, pipeline.getFileName(), pipeline.getFileSizeBytes(),
                        writerFactory.getWriter(pipeline.getFormat())
                );
            }
        }

        var errorMsg = pipeline.isTerminal()
                ? "does not complete properly"
                : "is still running";
        log.warn("[EXPORT] File stream unavailable - ID={}, reason: {}", pipelineId, errorMsg);
        throw new NoSuchFileException("Export pipeline %s [%d] %s".formatted(pipeline.getName(), pipeline.getId(), errorMsg));
    }

    /**
     * Retrieves available export fields for the protein detail DTO.
     *
     * @return list of exportable field schemas
     */
    public List<ExportFieldSchema> getAvailableFields() {
        log.debug("[EXPORT] Fetching available export fields");
        return ExportEngine.getAvailableFieldsForExport(ProteinDetailDto.class);
    }

    /**
     * Validates pipeline ownership and active status.
     *
     * @param pipelineId pipeline identifier
     * @param user       authenticated user
     * @return validated pipeline entity
     * @throws ResourceNotFoundException if not found
     * @throws ResourceDeletedException if soft-deleted
     * @throws AccessDeniedException    if not owned by user
     */
    private ExportPipeline findNotDeletedPipelineByIdAndOwner(Long pipelineId, UserPrincipal user) {
        log.debug("[EXPORT] Validating pipeline access - ID={}, user='{}'", pipelineId, user.id());

        var pipeline = pipelineRepository.findById(pipelineId);
        if (pipeline.isEmpty()) {
            log.warn("[EXPORT] Pipeline not found - ID={}", pipelineId);
            throw new ResourceNotFoundException("Pipeline with ID %d not found".formatted(pipelineId));
        }
        if (pipeline.get().isDeleted()) {
            log.warn("[EXPORT] Pipeline is soft-deleted - ID={}, deletedAt={}", pipelineId, pipeline.get().getDeletedAt());
            throw new ResourceDeletedException("Pipeline with ID %d deleted".formatted(pipelineId));
        }
        if (!pipeline.get().getUserId().equals(user.id())) {
            log.warn("[EXPORT] Access denied - pipeline belongs to different user - ID={}, owner='{}', requester='{}'",
                    pipelineId, pipeline.get().getUserId(), user.id());
            throw new AccessDeniedException("Pipeline with ID %d belongs to another user".formatted(pipelineId));
        }

        log.debug("[EXPORT] Pipeline validation passed - ID={}, status={}", pipelineId, pipeline.get().getStatus());
        return pipeline.get();
    }

    /**
     * Enqueues the pipeline job with Spring Batch.
     *
     * <p>Builds job parameters including:
     * <ul>
     *   <li>Pipeline ID and user context
     *   <li>Data provider (API/POSTGRES)
     *   <li>Export format and field selection
     * </ul>
     *
     * @param pipeline  created pipeline entity
     * @param initiator user context
     * @param fields    selected export fields
     */
    private void executePipelineJob(ExportPipeline pipeline, UserPrincipal initiator, List<String> fields) {
        var provider = DataProvider.isApi(initiator.dataProvider()) ? DataProvider.API : DataProvider.POSTGRES;

        log.info("[EXPORT] Building job parameters - ID={}, provider={}, format={}, fields.count={}",
                pipeline.getId(), provider.getKey(), pipeline.getFormat().name(), fields.size());

        try {
            var parameters = new JobParametersBuilder()
                    .addLong(Constants.EXPORT_JOB_ID.getKey(), pipeline.getId())
                    .addString(Constants.USER_ID.getKey(), initiator.id())
                    .addJobParameter(Constants.USER_ROLE.getKey(), initiator.roles(), List.class)
                    .addString(Constants.DATA_PROVIDER.getKey(), provider.getKey())
                    .addString(Constants.EXPORT_FORMAT.getKey(), pipeline.getFormat().name())
                    .addJobParameter(Constants.EXPORTED_FIELDS.getKey(), fields, List.class)
                    .toJobParameters();

            log.debug("[EXPORT] Submitting async job execution - ID={}", pipeline.getId());
            executor.execute(parameters);
            log.info("[EXPORT] Job submitted to async executor - ID={}", pipeline.getId());
        } catch (Exception e) {
            log.error("[EXPORT] Failed to start job - ID={}, exception: {}", pipeline.getId(), e.getMessage(), e);
            taskletService.markAsFailed(pipeline.getId(),
                    "Failed to start Export Pipeline %s <ID=%d> : %s".formatted(pipeline.getName(), pipeline.getId(), e.getMessage()));
            throw e;
        }
    }
}
