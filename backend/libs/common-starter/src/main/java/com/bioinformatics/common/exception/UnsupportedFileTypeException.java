package com.bioinformatics.common.exception;

/**
 * Thrown when an uploaded file has an unsupported MIME type or file extension.
 * Only .dat and .tsv files are accepted for UniProt imports.
 * Mapped to HTTP 422 Unprocessable Entity by {@link GlobalExceptionHandler}.
 *
 * @see GlobalExceptionHandler#handleUnsupportedFileTypeException(UnsupportedFileTypeException)
 */
public class UnsupportedFileTypeException extends RuntimeException {
    /**
     * Constructs an exception with the provided message.
     *
     * @param message description of the unsupported file type
     */
    public UnsupportedFileTypeException(String message) {
        super(message);
    }
}
