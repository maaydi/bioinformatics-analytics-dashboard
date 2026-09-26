package com.bioinformatics.common.exception;

import lombok.Builder;

import java.time.Instant;

/**
 * Standard error envelope returned by all REST API endpoints when an error occurs.
 * Provides consistent error reporting across the application for both client and operational debugging.
 *
 * <p>HTTP error responses are wrapped in this envelope to give clients:
 * <ul>
 *   <li>{@code status} — HTTP status code (e.g., 400, 401, 404, 500)</li>
 *   <li>{@code error} — HTTP status reason phrase (e.g., "Bad Request", "Not Found")</li>
 *   <li>{@code message} — Human-readable error description safe to display to end users</li>
 *   <li>{@code timestamp} — ISO 8601 timestamp of when the error occurred</li>
 * </ul>
 *
 * <p>Schema defined in documentation/api-contract.md — Shared Schemas — {@code ErrorResponse}.
 *
 * <p>Example response body:
 * <pre>{@code
 * {
 *   "status":    422,
 *   "error":     "Unprocessable Entity",
 *   "message":   "Filter name cannot be empty; length must be between 1 and 100 characters",
 *   "timestamp": "2026-04-27T14:30:00Z"
 * }
 * }</pre>
 *
 * @see GlobalExceptionHandler
 */
@Builder
public record ErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp
) {
}
