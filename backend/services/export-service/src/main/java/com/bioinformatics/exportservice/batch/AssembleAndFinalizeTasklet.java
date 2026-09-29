package com.bioinformatics.exportservice.batch;

import com.bioinformatics.common.providers.DataProvider;
import com.bioinformatics.exportservice.dto.DefaultExportFormat;
import com.bioinformatics.exportservice.dto.ExportFormat;
import com.bioinformatics.exportservice.dto.converter.ExportFormatRegistry;
import com.bioinformatics.exportservice.service.ExportFileStorageService;
import com.bioinformatics.exportservice.service.ExportPipelineLifeCycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.nio.file.Files;
import java.util.Objects;

import static com.bioinformatics.exportservice.dto.Constants.*;

/**
 * Spring Batch tasklet for assembling and finalizing export files.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Merge segment files written by chunk steps into final export file
 *   <li>Calculate final file size
 *   <li>Retrieve actual row count from previous step
 *   <li>Mark pipeline as COMPLETED with file metadata
 *   <li>Clean up temporary segments directory
 * </ul>
 *
 * <p>Runs as the final step in the job (after API or Postgres export step).
 * Delegates status updates to {@link ExportPipelineLifeCycleService}.
 *
 * @see ExportJobConfig
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssembleAndFinalizeTasklet implements Tasklet {

    private final ExportFileStorageService storageService;
    private final ExportPipelineLifeCycleService pipelineService;

    /**
     * Executes file assembly and finalization.
     *
     * <p>Flow:
     * <ol>
     *   <li>Extract job parameters (user, pipeline ID, format, data provider)
     *   <li>Assemble segment files into final export file
     *   <li>Calculate final file size in bytes
     *   <li>Retrieve write count from chunk step (actual rows)
     *   <li>Mark pipeline as COMPLETED
     *   <li>Clean up segments directory
     *   <li>Return FINISHED
     * </ol>
     *
     * @param contribution step contribution
     * @param chunkContext batch context (provides job parameters and step execution)
     * @return FINISHED when assembly completes
     * @throws Exception if file assembly fails
     */
    @Override
    public RepeatStatus execute(@NonNull StepContribution contribution, ChunkContext chunkContext) throws Exception {
        var jobParams = chunkContext.getStepContext().getStepExecution().getJobParameters();
        var userId = jobParams.getString(USER_ID.getKey());
        var pipelineId = jobParams.getLong(EXPORT_JOB_ID.getKey());
        var dataProvider = Objects.requireNonNull(jobParams.getString(DATA_PROVIDER.getKey()),
                "Data Provider Parameter is not defined");
        var format = getExportFormat(jobParams, DefaultExportFormat.CSV);

        log.info("[ASSEMBLE_FINALIZE] Starting assembly - pipelineId={}, userId={}, format={}, provider={}",
                pipelineId, userId, format, dataProvider);

        log.debug("[ASSEMBLE_FINALIZE] Assembling segment files - pipelineId={}", pipelineId);
        var finalFile = storageService.assembleSegments(userId, pipelineId, format);
        log.info("[ASSEMBLE_FINALIZE] Assembly complete - pipelineId={}, finalPath={}", pipelineId, finalFile);

        log.debug("[ASSEMBLE_FINALIZE] Calculating file size - pipelineId={}", pipelineId);
        var fileSizeBytes = storageService.getFileSize(userId, pipelineId, format);
        log.info("[ASSEMBLE_FINALIZE] File size calculated - pipelineId={}, size={} bytes", pipelineId, fileSizeBytes);

        log.debug("[ASSEMBLE_FINALIZE] Retrieving write count from step - pipelineId={}", pipelineId);
        var actualRows = getWriteCountFromPreviousStep(chunkContext,
                DataProvider.isApi(dataProvider) ? API_EXPORT_STEP.getKey() : POSTGRES_EXPORT_STEP.getKey());
        log.info("[ASSEMBLE_FINALIZE] Write count retrieved - pipelineId={}, actualRows={}", pipelineId, actualRows);

        log.debug("[ASSEMBLE_FINALIZE] Marking pipeline as COMPLETED - pipelineId={}, actualRows={}", pipelineId, actualRows);
        pipelineService.markAsCompleted(
                pipelineId,
                finalFile.toAbsolutePath().toString(),
                fileSizeBytes,
                actualRows
        );
        log.info("[ASSEMBLE_FINALIZE] Pipeline marked COMPLETED - pipelineId={}, finalSize={} bytes, rows={}, finalPath={}",
                pipelineId, fileSizeBytes, actualRows, finalFile);

        var segmentsDir = finalFile.getParent().resolve("segments");
        if (Files.exists(segmentsDir)) {
            log.debug("[ASSEMBLE_FINALIZE] Cleaning up segments directory - pipelineId={}, path={}",
                    pipelineId, segmentsDir);
            FileSystemUtils.deleteRecursively(segmentsDir);
            log.info("[ASSEMBLE_FINALIZE] Segments cleaned up - pipelineId={}", pipelineId);
        }

        log.info("[ASSEMBLE_FINALIZE] Finalization complete - pipelineId={}", pipelineId);
        return RepeatStatus.FINISHED;
    }

    /**
     * Extracts the exact number of items successfully written by the chunk step.
     *
     * <p>Searches job execution's step executions for the specified step name
     * and returns its write count.
     *
     * @param chunkContext batch context (contains job and step executions)
     * @param stepName     name of the step to query (e.g., "uniprot-api-export-step")
     * @return write count (number of items written), or 0 if step not found
     */
    private long getWriteCountFromPreviousStep(ChunkContext chunkContext, String stepName) {
        log.debug("[ASSEMBLE_FINALIZE] Querying step for write count - stepName={}", stepName);
        return chunkContext.getStepContext().getStepExecution().getJobExecution().getStepExecutions().stream()
                .filter(se -> se.getStepName().equals(stepName))
                .peek(se -> log.debug("[ASSEMBLE_FINALIZE] Step found - name={}, writeCount={}", se.getStepName(), se.getWriteCount()))
                .mapToLong(StepExecution::getWriteCount)
                .findFirst()
                .orElseGet(() -> {
                    log.warn("[ASSEMBLE_FINALIZE] Step not found - stepName={}, returning 0", stepName);
                    return 0L;
                });
    }

    /**
     * Resolves export format from job parameters.
     *
     * <p>Falls back to default if format parameter is missing or invalid.
     *
     * @param parameters    job parameters
     * @param defaultFormat fallback format
     * @return resolved export format
     */
    private ExportFormat getExportFormat(JobParameters parameters, ExportFormat defaultFormat) {
        var format = ExportFormatRegistry.getFormat(parameters.getString(EXPORT_FORMAT.getKey()));

        if (Objects.isNull(format)) {
            log.warn("[ASSEMBLE_FINALIZE] Format parameter not defined or invalid, using default - default={}",
                    defaultFormat);
            return defaultFormat;
        }
        log.debug("[ASSEMBLE_FINALIZE] Format resolved - format={}", format);
        return format;
    }
}
