package com.bioinformatics.dashboard.job.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Constants {

    MANUAL_IMPORT_JOB("uniProtImportJob"),
    IMPORT_STEP("uniProtImportStep"),
    IMPORT_JOB_ID("importUniprotJobId"),
    FILE_PATH("filePath"),
    TIMESTAMP("timestamp"),

    /**
     * API-based import job constants
     */
    AUTOMATIC_API_IMPORT_JOB("uniProtApiImportJob"),
    API_IMPORT_STEP("uniProtApiImportStep");

    private final String key;
}
