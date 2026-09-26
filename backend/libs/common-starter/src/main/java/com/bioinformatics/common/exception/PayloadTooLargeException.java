package com.bioinformatics.common.exception;

/**
 * Thrown when an uploaded file exceeds the maximum allowed size (2 GB).
 * This is a domain-specific exception that may wrap or complement
 * {@link org.springframework.web.multipart.MaxUploadSizeExceededException}.
 * Mapped to HTTP 413 Payload Too Large by {@link GlobalExceptionHandler}.
 *
 * @see GlobalExceptionHandler#handleFileTooLarge(org.springframework.web.multipart.MaxUploadSizeExceededException)
 */
public class PayloadTooLargeException extends RuntimeException {
    /**
     * Constructs an exception with the provided message.
     *
     * @param message description of the size violation (e.g., file size and limit)
     */
    public PayloadTooLargeException(String message) {
        super(message);
    }
}
