package com.bioinformatics.common.exception;

/**
 * Thrown when a background job (e.g., import job) fails during execution.
 * Indicates that the job encountered an unrecoverable error and cannot continue.
 * May be caught and handled by job orchestration logic or mapped to an HTTP error response.
 *
 * @see GlobalExceptionHandler#handleUnexpected(Exception)
 */
public class ExecuteJobException extends RuntimeException {
    /**
     * Constructs an exception with the provided message.
     *
     * @param message description of the job execution failure
     */
    public ExecuteJobException(String message) {
        super(message);
    }

    /**
     * Constructs an exception with the provided message and underlying cause.
     *
     * @param message description of the job execution failure
     * @param e       the underlying exception that caused the job to fail
     */
    public ExecuteJobException(String message, Throwable e) {
        super(message, e);
    }

}
