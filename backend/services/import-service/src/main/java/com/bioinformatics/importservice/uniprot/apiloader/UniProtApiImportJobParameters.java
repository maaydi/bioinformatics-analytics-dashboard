package com.bioinformatics.importservice.uniprot.apiloader;

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
public class UniProtApiImportJobParameters {


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


}
