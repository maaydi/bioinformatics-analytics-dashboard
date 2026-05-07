package com.bioinformatics.dashboard.batch;

import lombok.Getter;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@Getter
public class UniProtImportJobParameters {

    /**
     * defined in {@linkplain com.bioinformatics.dashboard.job.dto.Constants#FILE_PATH}
     *
     */
    @Value("#{jobParameters[filePath]}")
    private String filePath;

    @Value("#{jobParameters[importUniprotJobId]}")
    private String jobId;


}
