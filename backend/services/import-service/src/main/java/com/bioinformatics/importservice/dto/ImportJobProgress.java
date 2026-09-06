package com.bioinformatics.importservice.dto;

public record ImportJobProgress(String id, ImportStatus status, String fileName, int recordsProcessed,
        int totalEstimated, int progressPercent,
        long elapsedMs, String errorMessage

) {

}
