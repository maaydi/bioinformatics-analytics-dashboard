package com.bioinformatics.dashboard.job.dto;

import java.time.Instant;

public record ImportJobSummary(String id, ImportStatus status, String fileName, int progressPercent, int entryCount,
                               long durationMs,
                               Instant createdAt, Instant completedAt, String errorMessage) {
}
