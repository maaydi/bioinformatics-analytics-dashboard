package com.bioinformatics.dashboard.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Centralised exception handling — all unhandled exceptions are converted to
 * {@link ErrorResponse} envelopes here.
 *
 * <p>Rules:
 * <ul>
 *   <li>MethodArgumentNotValidException → 400 (DTO @Valid failures)</li>
 *   <li>ConstraintViolationException    → 400 (path/query param constraints)</li>
 *   <li>ResourceNotFoundException       → 404</li>
 *   <li>ImportAlreadyRunningException   → 409</li>
 *   <li>MaxUploadSizeExceededException  → 413 (file > 2 GB)</li>
 *   <li>IllegalArgumentException        → 422</li>
 *   <li>Exception (catch-all)           → 500 (message hidden from client)</li>
 * </ul>
 *
 * @see <a href="{@docRoot}/documentation/validation-rules.md">Validation Rules §10</a>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Handle Validation Exception {}", ex.getMessage(), ex);
        var message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Handle Constraint Violation {}", ex.getMessage(), ex);
        var message = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Handle notFound Exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ImportAlreadyRunningException.class)
    public ResponseEntity<ErrorResponse> handleImportConflict(ImportAlreadyRunningException ex) {
        log.warn("Handle ImportAlreadyRunning Exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFileTooLarge(MaxUploadSizeExceededException ex) {
        log.warn("Handle MaxUploadSizeExceeded Exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.CONTENT_TOO_LARGE,
                "File exceeds maximum allowed size of 2 GB");
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<Object> handleUnsupportedFileTypeException(UnsupportedFileTypeException ex) {
        log.warn("Handle UnsupportedFileTypeException Exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Handle IllegalArgumentException Exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ErrorResponse> handleAuthentication(RuntimeException ex) {
        log.warn("Handle Authentication Exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        if (isClientAbort(ex)) {
            log.debug("Client disconnected before the response was fully written: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        log.warn("Handle Unexpected Exception: {}", ex.getMessage(), ex);
        // Do NOT expose internal details to clients (OWASP A05)
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support.");
    }

    private boolean isClientAbort(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof AsyncRequestNotUsableException || current instanceof ClientAbortException) {
                return true;
            }

            var message = current.getMessage();
            if (message != null) {
                var normalizedMessage = message.toLowerCase(Locale.ROOT);
                if (normalizedMessage.contains("broken pipe")
                        || normalizedMessage.contains("relais brisé")
                        || normalizedMessage.contains("servletoutputstream failed to write")) {
                    return true;
                }
            }

            current = current.getCause();
        }
        return false;
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
        ErrorResponse body = ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
