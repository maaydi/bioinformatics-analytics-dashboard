package com.bioinformatics.dashboard.exception;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message, Throwable e) {
        super(message, e);
    }

    public RateLimitExceededException(String message) {
        super(message);
    }

}
