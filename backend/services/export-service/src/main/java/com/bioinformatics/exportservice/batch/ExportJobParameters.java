package com.bioinformatics.exportservice.batch;

import lombok.Getter;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@StepScope
@Getter
public class ExportJobParameters {


    /**
     * Current Job ID
     */
    @Value("#{jobParameters[jobId]}")
    private long jobId;


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
     * Export data provider
     */
    @Value("#{jobParameters[dataProvider]}")
    private String dataProvider;


}
