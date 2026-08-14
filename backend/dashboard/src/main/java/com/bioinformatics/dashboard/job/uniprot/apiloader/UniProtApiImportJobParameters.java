package com.bioinformatics.dashboard.job.uniprot.apiloader;

import lombok.Getter;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Manages operations and logic for UniProtApiImportJobParameters.
 */
@Component
@StepScope
@Getter
public class UniProtApiImportJobParameters {


    /**
     * Query from saved filter ID
     */
    @Value("#{jobParameters[filterId]}")
    private long filterId;


}
