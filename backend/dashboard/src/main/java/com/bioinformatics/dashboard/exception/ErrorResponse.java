package com.bioinformatics.dashboard.exception;

import lombok.Builder;

import java.time.Instant;

/**
 * Standard error envelope returned by the API for all error responses.
 *
 * <p>Schema defined in documentation/api-contract.md — Shared Schemas — {@code ErrorResponse}.
 *
 * <pre>{@code
 * {
 *   "status":    422,
 *   "error":     "Unprocessable Entity",
 *   "message":   "Human-readable description",
 *   "timestamp": "2026-04-27T14:30:00Z"
 * }
 * }</pre>
 */
@Builder
public record ErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp
) {
}
