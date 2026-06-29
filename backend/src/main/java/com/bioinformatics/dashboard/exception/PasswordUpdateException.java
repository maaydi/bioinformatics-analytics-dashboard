package com.bioinformatics.dashboard.exception;

public class PasswordUpdateException extends RuntimeException {
    public PasswordUpdateException(String message) {
        super(message);
    }

    public PasswordUpdateException(String message, Throwable throwable) {
        super(message, throwable);
    }

}
