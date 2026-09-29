package com.bioinformatics.common.exception;

/**
 * Thrown when a requested resource (e.g., protein entry, saved filter, import job) previously existed
 * but has been permanently deleted and is no longer available.
 * Semantically different from {@link ResourceNotFoundException} — this indicates the resource
 * existed in the past but is now gone (e.g., marked as stale or intentionally deleted).
 * Mapped to HTTP 410 Gone by {@link GlobalExceptionHandler}.
 *
 * @see ResourceNotFoundException
 * @see GlobalExceptionHandler#handleDeleted(ResourceDeletedException)
 */
public class ResourceDeletedException extends RuntimeException {

    /**
     * Constructs an exception with the provided message.
     *
     * @param message description of the deleted resource
     */
    public ResourceDeletedException(String message) {
        super(message);
    }
}
