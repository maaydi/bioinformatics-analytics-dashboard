package com.bioinformatics.importservice.uniprot;

import lombok.Getter;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Batch-scoped component for accessing and managing job execution parameters.
 *
 * <p>Parameters are injected from Spring Batch {@code JobParameters} at step execution time.
 * All fields are lazy-loaded and available only within a step context.
 *
 * <p>Available parameters:
 * <ul>
 *   <li>{@code filePath} – local file path for file-based imports
 *   <li>{@code importUniprotJobId} – unique job identifier (UUID)
 *   <li>{@code filterId} – saved filter ID for API-based imports
 *   <li>{@code initiatorUserId} – authenticated user requesting import
 *   <li>{@code initiatorRole} – user roles for authorization
 *   <li>{@code dataProvider} – source (FILE or API)
 * </ul>
 *
 * @see com.bioinformatics.importservice.uniprot.ImportJobConfig
 */
@Component
@StepScope
@Getter
public class ImportJobParameters {
    /**
     * Job file path for file-based imports (job parameter: {@code filePath}).
     * Only set for FILE provider; null for API provider.
     *
     * @see com.bioinformatics.importservice.dto.Constants#FILE_PATH
     */
    @Value("#{jobParameters[filePath]}")
    private String filePath;

    /**
     * Unique import job identifier (job parameter: {@code importUniprotJobId}).
     * Used as foreign key to ImportJob entity for progress tracking and final status update.
     */
    @Value("#{jobParameters[importUniprotJobId]}")
    private String jobId;

    /**
     * Saved filter identifier for API-based imports (job parameter: {@code filterId}).
     * Only set for API provider; 0 or null for FILE provider.
     */
    @Value("#{jobParameters[filterId]}")
    private long filterId;

    /**
     * Authenticated user requesting the import (job parameter: {@code initiatorUserId}).
     * Used for permission checks and audit logging.
     */
    @Value("#{jobParameters[initiatorUserId]}")
    private String initiatorUserId;

    /**
     * User authorization roles (job parameter: {@code initiatorRole}).
     * List of role strings (e.g., [ADMIN, USER]). Used for downstream authorization.
     */
    @Value("#{jobParameters[initiatorRole]}")
    private List<String> initiatorRole;

    /**
     * Data provider source (job parameter: {@code dataProvider}).
     * Values: "FILE" (file-based) or "API" (UniProt API remote import).
     */
    @Value("#{jobParameters[dataProvider]}")
    private String dataProvider;



}
