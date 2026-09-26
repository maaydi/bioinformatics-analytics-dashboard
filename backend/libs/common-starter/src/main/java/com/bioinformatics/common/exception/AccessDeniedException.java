package com.bioinformatics.common.exception;

/**
 * Thrown when a user lacks the required permissions to access a resource or perform an action.
 * Typically raised for authorization failures (e.g., non-ADMIN accessing {@code /api/admin/**} endpoints).
 * Mapped to HTTP 403 Forbidden by {@link GlobalExceptionHandler}.
 *
 * @see AccessDeniedException
 * @see GlobalExceptionHandler#handleAccessDenied(AccessDeniedException)
 */
public class AccessDeniedException extends RuntimeException {
    /**
     * Constructs an exception with the provided message.
     *
     * @param message human-readable reason for access denial
     */
    public AccessDeniedException(String message) {
        super(message);
    }

}
