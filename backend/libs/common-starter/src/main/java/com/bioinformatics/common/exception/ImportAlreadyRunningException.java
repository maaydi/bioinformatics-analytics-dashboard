package com.bioinformatics.common.exception;

/**
 * Thrown when a second import is attempted while one is already RUNNING.
 * Mapped to HTTP 409 by {@link GlobalExceptionHandler}.
 *
 * <p>See documentation/overview.md US-3 and api-contract.md §3.
 */
public class ImportAlreadyRunningException extends RuntimeException {


    public ImportAlreadyRunningException(String runningJobId) {
        super("An import job is already running " + runningJobId);
    }

}
