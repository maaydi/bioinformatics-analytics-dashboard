package com.bioinformatics.exportservice.batch;

import com.bioinformatics.common.config.CommonProperties;
import com.bioinformatics.common.providers.DataProvider;
import com.bioinformatics.exportservice.client.GeneService;
import com.bioinformatics.exportservice.config.ApplicationProperties;
import com.bioinformatics.exportservice.service.ExportPipelineLifeCycleService;
import com.bioinformatics.shared.models.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * Spring Batch tasklet for validating and estimating export pipeline data.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Fetch filter criteria from pipeline
 *   <li>Count matching records (estimated rows)
 *   <li>Validate that data exists (fail early if no data)
 *   <li>Warn if data exceeds max-rows limit
 *   <li>Calculate chunk count for progress tracking
 * </ul>
 *
 * <p>Runs in the {@code validate-and-estimate} step (first step in job).
 * Updates pipeline estimated rows and job execution chunks total.
 *
 * @see ExportJobConfig
 */
@Component
@Slf4j
@StepScope
@RequiredArgsConstructor
public class ValidateAndEstimateTasklet implements Tasklet {

    private final ExportJobParameters jobParameters;
    private final ExportPipelineLifeCycleService exportPipelineService;
    private final GeneService geneService;
    private final ApplicationProperties applicationProperties;
    private final CommonProperties commonProperties;

    /**
     * Executes validation and estimation.
     *
     * <p>Flow:
     * <ol>
     *   <li>Retrieve pipeline filter and user context
     *   <li>Determine chunk size based on data provider (API or DB)
     *   <li>Count matching records via GeneService
     *   <li>Validate: count > 0 (fail early if no data)
     *   <li>Warn: if count > application.export.max-rows
     *   <li>Update pipeline estimated rows and chunk count
     *   <li>Return FINISHED for successful completion
     * </ol>
     *
     * @param contribution step contribution for exit status
     * @param chunkContext batch context (not directly used here)
     * @return FINISHED if validation succeeds
     * @throws IllegalStateException if no data to export
     */
    @Override
    public @Nullable RepeatStatus execute(@NonNull StepContribution contribution, @NonNull ChunkContext chunkContext) {
        var pipelineId = jobParameters.getJobId();

        log.info("[VALIDATE_ESTIMATE] Starting validation and estimation - pipelineId={}", pipelineId);

        var filterJson = exportPipelineService.getExportPipelineSearchRequest(pipelineId);
        log.debug("[VALIDATE_ESTIMATE] Filter retrieved - pipelineId={}, filter type={}",
                pipelineId, filterJson.getClass().getSimpleName());

        var user = new UserPrincipal(
                jobParameters.getInitiatorUserId(),
                jobParameters.getInitiatorRole(),
                jobParameters.getDataProvider()
        );
        log.debug("[VALIDATE_ESTIMATE] User context loaded - userId={}, dataProvider={}, roles={}",
                jobParameters.getInitiatorUserId(), jobParameters.getDataProvider(), jobParameters.getInitiatorRole());

        var chunkSize = DataProvider.isApi(jobParameters.getDataProvider())
                ? commonProperties.uniprotApi().batch().chunkSize()
                : applicationProperties.export().batch().chunkSize();
        log.debug("[VALIDATE_ESTIMATE] Chunk size determined - dataProvider={}, chunkSize={}",
                jobParameters.getDataProvider(), chunkSize);

        log.info("[VALIDATE_ESTIMATE] Counting records - pipelineId={}, filter={}", pipelineId, filterJson);
        long estimatedRows = geneService.count(filterJson, user);
        log.info("[VALIDATE_ESTIMATE] Count result - pipelineId={}, estimatedRows={}", pipelineId, estimatedRows);

        if (estimatedRows == 0) {
            log.error("[VALIDATE_ESTIMATE] No data found - pipelineId={}, filter={}", pipelineId, filterJson);
            exportPipelineService.updatePipelineEstimatedRows(pipelineId, 0L, chunkSize);
            contribution.setExitStatus(new ExitStatus("NO_DATA"));
            throw new IllegalStateException("Pipeline failed: No data to export for request %s".formatted(filterJson));
        }

        if (estimatedRows > applicationProperties.export().maxRows()) {
            log.warn("[VALIDATE_ESTIMATE] Data exceeds max limit - pipelineId={}, estimatedRows={}, maxRows={}, proceeding anyway",
                    pipelineId, estimatedRows, applicationProperties.export().maxRows());
        }

        log.debug("[VALIDATE_ESTIMATE] Updating pipeline with estimates - pipelineId={}, estimatedRows={}, chunkSize={}",
                pipelineId, estimatedRows, chunkSize);
        exportPipelineService.updatePipelineEstimatedRows(pipelineId, estimatedRows, chunkSize);
        log.info("[VALIDATE_ESTIMATE] Validation and estimation complete - pipelineId={}, estimatedRows={}",
                pipelineId, estimatedRows);

        return RepeatStatus.FINISHED;
    }
}
