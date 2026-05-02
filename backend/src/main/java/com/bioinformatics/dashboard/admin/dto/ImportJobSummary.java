package com.bioinformatics.dashboard.admin.dto;

import java.time.LocalDateTime;

public record ImportJobSummary(String id, ImportStatus status, String fileName, int entryCount, long durationMs,
                LocalDateTime createdAt, LocalDateTime completedAt, String errorMessage) {
}
