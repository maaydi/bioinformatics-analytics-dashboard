package com.bioinformatics.importservice.uniprot;

import lombok.Getter;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Manages operations and logic for UniProtApiImportJobParameters.
 */
@Component
@StepScope
@Getter
public class ImportJobParameters {
    /**
     * Job file path (job parameter: filePath). See Constants#FILE_PATH.
     */
    @Value("#{jobParameters[filePath]}")
    private String filePath;

    /**
     * Import job id (job parameter: importUniprotJobId)
     */
    @Value("#{jobParameters[importUniprotJobId]}")
    private String jobId;

    /**
     * Query from saved filter ID
     */
    @Value("#{jobParameters[filterId]}")
    private long filterId;

    /**
     * Initiator user id
     */
    @Value("#{jobParameters[initiatorUserId]}")
    private String initiatorUserId;

    /**
     * Initiator user roles
     */
    @Value("#{jobParameters[initiatorRole]}")
    private List<String> initiatorRole;

    /**
     * Import data provider
     */
    @Value("#{jobParameters[dataProvider]}")
    private String dataProvider;



}
