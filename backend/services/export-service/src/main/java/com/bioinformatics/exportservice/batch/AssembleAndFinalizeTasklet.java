package com.bioinformatics.exportservice.batch;

import com.bioinformatics.exportservice.dto.DefaultExportFormat;
import com.bioinformatics.exportservice.service.ExportFileStorageService;
import com.bioinformatics.exportservice.service.ExportPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class AssembleAndFinalizeTasklet implements Tasklet {

    private final ExportFileStorageService storageService;
    private final ExportPipelineService pipelineService;

    @Override
    public RepeatStatus execute(@NonNull StepContribution contribution, ChunkContext chunkContext) throws Exception {
        var jobParams = chunkContext.getStepContext().getStepExecution().getJobParameters();
        var userId = jobParams.getString(USER_ID.getKey());
        var pipelineId = jobParams.getLong(EXPORT_JOB_ID.getKey());
        var formatStr = jobParams.getString(EXPORT_FORMAT.getKey());
        var dataProvider = Objects.requireNonNull(jobParams.getString(DATA_PROVIDER.getKey()), "Data Provider Parameter is not defined");
        var format = DefaultExportFormat.valueOf(formatStr);

        log.info("Starting assembly for pipeline {} (format: {})", pipelineId, format);

        var finalFile = storageService.assembleSegments(userId, pipelineId, format);
        var fileSizeBytes = storageService.getFileSize(userId, pipelineId, format);

        var actualRows = getWriteCountFromPreviousStep(chunkContext,
                dataProvider.equalsIgnoreCase(API.getKey()) ? API_EXPORT_STEP.getKey() : POSTGRES_EXPORT_STEP.getKey());

        pipelineService.markAsCompleted(
                pipelineId,
                finalFile.toAbsolutePath().toString(),
                fileSizeBytes,
                actualRows
        );
        log.info("Pipeline {} marked as COMPLETED. Final size: {} bytes, Rows: {}", pipelineId, fileSizeBytes, actualRows);

        var segmentsDir = finalFile.getParent().resolve("segments");
        if (Files.exists(segmentsDir)) {
            FileSystemUtils.deleteRecursively(segmentsDir);
            log.debug("Cleaned up segments directory: {}", segmentsDir);
        }

        return RepeatStatus.FINISHED;
    }

    /**
     * Extracts the exact number of items successfully written by the chunk step.
     */
    private long getWriteCountFromPreviousStep(ChunkContext chunkContext, String stepName) {
        return chunkContext.getStepContext().getStepExecution().getJobExecution().getStepExecutions().stream()
                .filter(se -> se.getStepName().equals(stepName))
                .mapToLong(StepExecution::getWriteCount)
                .findFirst()
                .orElse(0L);
    }
}
