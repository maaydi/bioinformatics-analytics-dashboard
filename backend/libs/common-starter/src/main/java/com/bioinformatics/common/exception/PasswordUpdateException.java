package com.bioinformatics.common.exception;

/**
 * Thrown when a password update operation fails (e.g., BCrypt hashing error, database update failure).
 * Typically a server-side error that may indicate infrastructure or configuration issues.
 * Mapped to HTTP 500 Internal Server Error by {@link GlobalExceptionHandler}.
 *
 * @see GlobalExceptionHandler#handlePasswordUpdateException(PasswordUpdateException)
 */
public class PasswordUpdateException extends RuntimeException {
    /**
     * Constructs an exception with the provided message.
     *
     * @param message description of the password update failure
     */
    public PasswordUpdateException(String message) {
        super(message);
    }

    /**
     * Constructs an exception with the provided message and underlying cause.
     *
     * @param message   description of the password update failure
     * @param throwable the underlying exception that caused the update to fail
     */
    public PasswordUpdateException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
