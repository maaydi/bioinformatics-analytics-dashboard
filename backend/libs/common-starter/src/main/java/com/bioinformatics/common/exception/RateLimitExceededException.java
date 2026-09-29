package com.bioinformatics.common.exception;

/**
 * Thrown when a user or client exceeds the configured rate limit for an operation.
 * Signals that the request should be retried after some backoff period.
 * Mapped to HTTP 429 Too Many Requests by {@link GlobalExceptionHandler}.
 *
 * @see GlobalExceptionHandler#handleRateLimitExceeded(RateLimitExceededException)
 */
public class RateLimitExceededException extends RuntimeException {
    /**
     * Constructs an exception with the provided message and underlying cause.
     *
     * @param message description of the rate limit violation
     * @param e       the underlying exception cause
     */
    public RateLimitExceededException(String message, Throwable e) {
        super(message, e);
    }

    /**
     * Constructs an exception with the provided message.
     *
     * @param message description of the rate limit violation
     */
    public RateLimitExceededException(String message) {
        super(message);
    }

}
