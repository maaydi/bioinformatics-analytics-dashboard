package com.bioinformatics.importservice.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Constants {

    UNIPROT_IMPORT_JOB("uniProtImportJob"),
    IMPORT_STEP("uniProtImportStep"),
    /**
     * import Job Parameters
     */
    IMPORT_JOB_ID("importUniprotJobId"),
    FILE_PATH("filePath"),
    TIMESTAMP("timestamp"),
    SAVED_FILTER_ID("filterId"),
    USER_ID("initiatorUserId"),
    USER_ROLE("initiatorRole"),
    /**
     * Data provider for import jobs
     */
    DATA_PROVIDER("dataProvider"),
    FILE("file"),
    API("api"),

    /**
     * API-based import job Step
     */
    API_IMPORT_STEP("uniProtApiImportStep");

    private final String key;
}
