package com.bioinformatics.exportservice.batch;

import com.bioinformatics.exportservice.client.GeneService;
import com.bioinformatics.exportservice.config.ApplicationProperties;
import com.bioinformatics.exportservice.dto.ExportStatus;
import com.bioinformatics.exportservice.repository.ExportPipelineRepository;
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

@Component
@Slf4j
@StepScope
@RequiredArgsConstructor
public class ValidateAndEstimateTasklet implements Tasklet {

    private final ExportJobParameters jobParameters;
    private final ExportPipelineRepository pipelineRepository;
    private final GeneService geneService;
    private final ApplicationProperties applicationProperties;

    @Override
    public @Nullable RepeatStatus execute(@NonNull StepContribution contribution, @NonNull ChunkContext chunkContext) {
        log.info("Start to execute ValidateAndEstimateTasklet");

        var job = pipelineRepository.findById(jobParameters.getJobId())
                .orElseThrow(() -> new IllegalArgumentException("Export Pipeline with id %d not found".formatted(jobParameters.getJobId())));

        var user = new UserPrincipal(
                jobParameters.getInitiatorUserId(),
                jobParameters.getInitiatorRole(),
                jobParameters.getDataProvider()
        );

        var filterJson = job.getFilterJson();

        long estimatedRows = geneService.count(filterJson, user);

        if (estimatedRows == 0) {
            job.setEstimatedRows(0L);
            job.setStatus(ExportStatus.FAILED);
            pipelineRepository.save(job);

            contribution.setExitStatus(new ExitStatus("NO_DATA"));
            throw new IllegalStateException("Pipeline failed: No data to export for request %s".formatted(filterJson));
        }

        if (estimatedRows > applicationProperties.export().maxRows()) {
            log.warn("Exported data rows {} are greater than max-rows {} supported by application",
                    estimatedRows, applicationProperties.export().maxRows());
        }

        job.setEstimatedRows(estimatedRows);
        job.setStatus(ExportStatus.RUNNING);
        pipelineRepository.save(job);

        return RepeatStatus.FINISHED;
    }
}
