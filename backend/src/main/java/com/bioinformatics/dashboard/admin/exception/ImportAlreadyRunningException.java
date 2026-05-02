package com.bioinformatics.dashboard.admin.exception;

public class ImportAlreadyRunningException extends RuntimeException {
    public ImportAlreadyRunningException(String message) { super(message); }
}
