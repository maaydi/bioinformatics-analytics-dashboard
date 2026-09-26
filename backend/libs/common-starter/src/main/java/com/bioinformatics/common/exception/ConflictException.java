package com.bioinformatics.common.exception;

/**
 * Thrown when a request conflicts with the current state of the resource or business logic.
 * Examples: attempting to trigger an import while another import is already running,
 * or attempting to create a resource with a duplicate key.
 * Mapped to HTTP 409 Conflict by {@link GlobalExceptionHandler}.
 *
 * @see GlobalExceptionHandler#handleConflict(ConflictException)
 */
public class ConflictException extends RuntimeException {

    /**
     * Constructs an exception with the provided message.
     *
     * @param message description of the conflicting state or action
     */
    public ConflictException(String message) {
        super(message);
    }
}
