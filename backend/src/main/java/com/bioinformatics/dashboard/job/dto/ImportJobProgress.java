package com.bioinformatics.dashboard.job.dto;

public record ImportJobProgress(String id, ImportStatus status, String fileName, int recordsProcessed,
        int totalEstimated, int progressPercent,
        long elapsedM, String errorMessage

) {

}
