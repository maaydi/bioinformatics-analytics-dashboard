package com.bioinformatics.importservice.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Constants {

    IMPORT_FILE_JOB("uniProtImportJob"),
    IMPORT_STEP("uniProtImportStep"),
    IMPORT_JOB_ID("importUniprotJobId"),
    FILE_PATH("filePath"),
    TIMESTAMP("timestamp"),
    SAVED_FILTER_ID("filterId"),

    /**
     * API-based import job constants
     */
    IMPORT_API_JOB("uniProtApiImportJob"),
    API_IMPORT_STEP("uniProtApiImportStep");

    private final String key;
}
