package com.bioinformatics.dashboard.exception;

public class DuplicateFilterNameException extends RuntimeException {
    public DuplicateFilterNameException(String message, Throwable e) {
        super(message, e);
    }

}
