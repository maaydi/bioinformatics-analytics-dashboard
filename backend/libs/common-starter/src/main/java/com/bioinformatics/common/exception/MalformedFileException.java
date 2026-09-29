package com.bioinformatics.common.exception;

/**
 * Thrown when an uploaded file (e.g., UniProt .dat or .tsv) is malformed or contains invalid data
 * that cannot be parsed according to the expected format specification.
 * Typically maps to HTTP 422 Unprocessable Entity by {@link GlobalExceptionHandler}.
 *
 * @see GlobalExceptionHandler#handleIllegalArgument(IllegalArgumentException)
 */
public class MalformedFileException extends RuntimeException {
    /**
     * Constructs an exception with the provided message.
     *
     * @param message description of the parsing error or format violation
     */
    public MalformedFileException(String message) {
        super(message);
    }
}
