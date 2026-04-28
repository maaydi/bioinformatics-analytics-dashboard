package com.bioinformatics.dashboard.exception;

/**
 * Thrown when a second import is attempted while one is already RUNNING.
 * Mapped to HTTP 409 by {@link GlobalExceptionHandler}.
 *
 * <p>See documentation/overview.md US-3 and api-contract.md §3.
 */
public class ImportAlreadyRunningException extends RuntimeException {

    private final String runningJobId;

    public ImportAlreadyRunningException(String runningJobId) {
        super("An import job is already running");
        this.runningJobId = runningJobId;
    }

    public String getRunningJobId() {
        return runningJobId;
    }
}
