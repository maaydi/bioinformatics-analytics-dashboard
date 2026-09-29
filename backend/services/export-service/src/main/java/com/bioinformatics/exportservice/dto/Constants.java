package com.bioinformatics.exportservice.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Constants {

    UNIPROT_EXPORT_JOB("uniProtExportJob"),
    POSTGRES_EXPORT_STEP("uniProtPostgresExportStep"),
    API_EXPORT_STEP("uniProtApiExportStep"),
    /**
     * Tasks
     *
     */
    VALIDATE_ESTIMATE_TASK("validateAndEstimateStep"),
    ASSEMBLE_FINALIZE_TASK("assembleAndFinalizeStep"),
    /**
     * export Job Parameters
     */
    EXPORT_JOB_ID("jobId"),
    USER_ID("initiatorUserId"),
    USER_ROLE("initiatorRole"),
    EXPORT_FORMAT("exportFormat"),
    EXPORTED_FIELDS("exportedFields"),
    /**
     * Data provider for export jobs
     */
    DATA_PROVIDER("dataProvider");

    private final String key;
}
