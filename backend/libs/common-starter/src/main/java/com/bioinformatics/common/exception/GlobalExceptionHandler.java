package com.bioinformatics.common.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
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
        log.warn("Handle Validation Exception {}", ex.getMessage());
        var message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Handle Constraint Violation {}", ex.getMessage());
        var message = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining("; "));
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Handle notFound Exception: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ImportAlreadyRunningException.class)
    public ResponseEntity<ErrorResponse> handleImportConflict(ImportAlreadyRunningException ex) {
        log.warn("Handle ImportAlreadyRunning Exception: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFileTooLarge(MaxUploadSizeExceededException ex) {
        log.warn("Handle MaxUploadSizeExceeded Exception: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONTENT_TOO_LARGE,
                "File exceeds maximum allowed size of 2 GB");
    }

    @ExceptionHandler(ExportRowCapExceededException.class)
    public ResponseEntity<ErrorResponse> handleExportRowCapExceeded(ExportRowCapExceededException ex) {
        log.warn("Handle ExportRowCapExceeded Exception: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONTENT_TOO_LARGE, ex.getMessage());
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<Object> handleUnsupportedFileTypeException(UnsupportedFileTypeException ex) {
        log.warn("Handle UnsupportedFileTypeException Exception: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Handle IllegalArgumentException Exception: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(RuntimeException ex) {
        log.warn("Handle Authentication Exception: {} {}", ex.getClass().getName(), ex.getMessage(), ex);
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

    @ExceptionHandler(DuplicateFilterNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatedFilterName(DuplicateFilterNameException ex) {
        log.warn("Handle Duplicate Filter Name Exception: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "Duplicate filter name");
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Handle Access Denied Exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.FORBIDDEN, "Access Denied");
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RateLimitExceededException ex) {
        log.warn("Handle Rate limit exceeded Exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Try again later.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        log.warn("Handle ResponseStatus Exception: {}", ex.getMessage());
        var status = Objects.requireNonNullElse(HttpStatus.resolve(ex.getStatusCode().value()), HttpStatus.INTERNAL_SERVER_ERROR);
        return buildResponse(status, ex.getReason());
    }

    @ExceptionHandler(PasswordUpdateException.class)
    public ResponseEntity<ErrorResponse> handlePasswordUpdateException(PasswordUpdateException ex) {
        log.warn("Handle Password Update Exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private boolean isClientAbort(Throwable throwable) {
        var current = throwable;
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
