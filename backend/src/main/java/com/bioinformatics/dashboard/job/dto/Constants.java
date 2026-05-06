package com.bioinformatics.dashboard.job.dto;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Constants {
    IMPORT_JOB("uniProtImportJob"),
    IMPORT_STEP("uniProtImportStep"),
    IMPORT_JOB_ID("importUniprotJobId"),
    FILE_PATH("filePath"),
    TIMESTAMP("timestamp");

    private final String name;
}
