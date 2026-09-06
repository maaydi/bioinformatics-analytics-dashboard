package com.bioinformatics.common.exception;

public class DuplicateFilterNameException extends RuntimeException {
    public DuplicateFilterNameException(String message, Throwable e) {
        super(message, e);
    }

}
