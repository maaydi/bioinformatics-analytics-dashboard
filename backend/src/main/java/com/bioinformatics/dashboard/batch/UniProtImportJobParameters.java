package com.bioinformatics.dashboard.batch;

import lombok.Getter;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Manages operations and logic for UniProtImportJobParameters.
 */
@Component
@StepScope
@Getter
public class UniProtImportJobParameters {

    /**
     * Job file path (job parameter: filePath). See Constants#FILE_PATH.
     */
    @Value("#{jobParameters[filePath]}")
    private String filePath;

    /** Import job id (job parameter: importUniprotJobId) */
    @Value("#{jobParameters[importUniprotJobId]}")
    private String jobId;


}
