package com.bioinformatics.common.exception;

/**
 * Thrown when a user attempts to create or update a saved filter with a name that already exists
 * for that user. Each user must maintain unique filter names in their personal filter collection.
 * Mapped to HTTP 409 Conflict by {@link GlobalExceptionHandler}.
 *
 * @see GlobalExceptionHandler#handleDuplicatedFilterName(DuplicateFilterNameException)
 */
public class DuplicateFilterNameException extends RuntimeException {
    /**
     * Constructs an exception with the provided message and underlying cause.
     *
     * @param message description of the duplicate filter name error
     * @param e       the underlying exception cause
     */
    public DuplicateFilterNameException(String message, Throwable e) {
        super(message, e);
    }

}
