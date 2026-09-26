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
import com.bioinformatics.exportservice.entity.ExportJobExecution;
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


    public ExportPipelineResponse createPipeline(ExportPipelineCreateRequest request, UserPrincipal initiator) {
        log.info("Creating Pipeline with name {} and description {}", request.name(), request.description());
        var pipeline = mapper.toEntity(request, initiator.id());
        var result = pipelineRepository.save(pipeline);
        log.info("Creating Job Execution for pipeline {}", request.name());
        var exec = new ExportJobExecution();
        exec.setPipeline(result);
        jobExecutionRepository.save(exec);
        return executePipelineJob(result, initiator, request.fieldSchema());
    }

    public PagedResponse<ExportPipelineResponse> listPipelines(ExportStatus status, Pageable pageable, UserPrincipal user) {
        var page = (status != null)
                ? pipelineRepository.findByUserIdAndStatusAndDeletedAtIsNull(user.id(), status, pageable)
                : pipelineRepository.findByUserIdAndDeletedAtIsNull(user.id(), pageable);
        return PagedResponse.of(page.map(mapper::toDto));
    }

    public ExportPipelineResponse getPipeline(Long pipelineId, UserPrincipal user) {
        log.info("Getting Pipeline with id {}", pipelineId);
        var pipeline = findNotDeletedPipelineByIdAndOwner(pipelineId, user);
        return mapper.toDto(pipeline);
    }

    public ExportJobStatusResponse getPipelineStatus(Long pipelineId, UserPrincipal user) {
        log.info("Get export job status for pipeline with  id {}", pipelineId);
        var pipeline = findNotDeletedPipelineByIdAndOwner(pipelineId, user);
        var execution = jobExecutionRepository.findByPipelineId(pipelineId);
        if (execution.isPresent()) {
            var currentStep = ExportJobStepTracker.determineCurrentStep(jobRepository.getJobExecution(pipeline.getJobExecutionId()));
            return new ExportJobStatusResponse(pipelineId,
                    pipeline.getStatus(),
                    execution.get().getProgressPercent(),
                    execution.get().getChunksProcessed(),
                    execution.get().getChunksTotal(),
                    currentStep,
                    execution.get().getUpdatedAt()
            );
        } else {
            log.info("No execution found for pipeline with ID {}", pipelineId);
            return new ExportJobStatusResponse(pipelineId, pipeline.getStatus(), 0, 0, 0, null, Instant.now());
        }
    }

    public DownloadUrlDto getDownloadUrl(Long pipelineId, UserPrincipal user) throws IOException {
        var pipeline = findNotDeletedPipelineByIdAndOwner(pipelineId, user);
        if (pipeline.isCompleted()) {
            return new DownloadUrlDto("/exports/pipelines/%d/download-file".formatted(pipelineId),
                    pipeline.getFileName(), pipeline.getFileSizeBytes(), pipeline.getFormat().getContentType());
        }
        throw new NoSuchFileException("Export pipeline %s [%d] %s".formatted(pipeline.getName(), pipeline.getId(),
                pipeline.isTerminal() ? "does not complete properly." : "is still running."));

    }

    public ExportPipelineResponse retryPipeline(Long pipelineId, UserPrincipal user) {
        var pipeline = findNotDeletedPipelineByIdAndOwner(pipelineId, user);
        if (!pipeline.isTerminal()) {
            throw new ConflictException("Export pipeline job %s [%d] is still running.".formatted(pipeline.getName(), pipelineId));
        }
        log.info("Retry execution for pipeline {} [{}]", pipeline.getName(), pipelineId);
        var dto = mapper.toDto(pipeline);
        var clonedPipeline = new ExportPipelineCreateRequest(dto.name(), dto.description(), dto.filter(), dto.format(), dto.fieldSchema());
        return createPipeline(clonedPipeline, user);
    }

    public void deletePipeline(Long pipelineId, UserPrincipal user) {
        var pipeline = findNotDeletedPipelineByIdAndOwner(pipelineId, user);
        if (!pipeline.isTerminal()) {
            try {
                var execution = jobRepository.getJobExecution(pipeline.getJobExecutionId());
                if (execution != null) {
                    executor.stop(execution);
                } else {
                    log.warn("No execution found for pipeline with ID {}", pipelineId);
                }
            } catch (Exception e) {
                // ignore not running exception
            }
        }
        pipeline.setDeletedAt(Instant.now());
    }

    public ExportFileStream getExportFileStream(Long pipelineId, UserPrincipal user) throws IOException {
        var pipeline = findNotDeletedPipelineByIdAndOwner(pipelineId, user);
        if (pipeline.isCompleted()) {
            var file = Paths.get(pipeline.getFilePath());
            if (Files.exists(file)) {
                var steam = Files.newInputStream(file, StandardOpenOption.READ);
                return new ExportFileStream(
                        steam, pipeline.getFileName(), pipeline.getFileSizeBytes(),
                        writerFactory.getWriter(pipeline.getFormat())
                );
            }
        }
        throw new NoSuchFileException("Export pipeline %s [%d] %s".formatted(pipeline.getName(), pipeline.getId(),
                pipeline.isTerminal() ? "does not complete properly." : "is still running."));
    }

    public List<ExportFieldSchema> getAvailableFields() {
        return ExportEngine.getAvailableFieldsForExport(ProteinDetailDto.class);
    }

    private ExportPipeline findNotDeletedPipelineByIdAndOwner(Long pipelineId, UserPrincipal user) {
        var pipeline = pipelineRepository.findById(pipelineId);
        if (pipeline.isEmpty()) {
            log.warn("Pipeline with id {} not found", pipelineId);
            throw new ResourceNotFoundException("Pipeline with ID %d not found".formatted(pipelineId));
        }
        if (pipeline.get().isDeleted()) {
            log.warn("Pipeline with id {} has been deleted", pipelineId);
            throw new ResourceDeletedException("Pipeline with ID %d deleted".formatted(pipelineId));
        }
        if (!pipeline.get().getUserId().equals(user.id())) {
            log.warn("Pipeline with id {} belongs to another user", pipelineId);
            throw new AccessDeniedException("Pipeline with ID %d belongs to another user".formatted(pipelineId));
        }
        return pipeline.get();
    }

    private ExportPipelineResponse executePipelineJob(ExportPipeline pipeline, UserPrincipal initiator, List<String> fields) {
        var provider = DataProvider.isApi(initiator.dataProvider()) ? DataProvider.API : DataProvider.POSTGRES;
        try {
            var parameters = new JobParametersBuilder()
                    .addLong(Constants.EXPORT_JOB_ID.getKey(), pipeline.getId())
                    .addString(Constants.USER_ID.getKey(), initiator.id())
                    .addJobParameter(Constants.USER_ROLE.getKey(), initiator.roles(), List.class)
                    .addString(Constants.DATA_PROVIDER.getKey(), provider.getKey())
                    .addJobParameter(Constants.EXPORT_FORMAT.getKey(), pipeline.getFormat(), ExportFormat.class)
                    .addJobParameter(Constants.EXPORTED_FIELDS.getKey(), fields, List.class)
                    .toJobParameters();
            executor.execute(parameters);
            return mapper.toDto(pipeline);
        } catch (Exception e) {
            taskletService.markAsFailed(pipeline.getId(), "Failed to start Export Pipeline %s <ID=%d> : %s".formatted(pipeline.getName(), pipeline.getId(), e.getMessage()));
            throw e;
        }
    }
}
