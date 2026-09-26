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
 * Centralized exception handling for all REST endpoints — converts unhandled exceptions
 * into standardized {@link ErrorResponse} envelopes before returning to clients.
 *
 * <p><strong>Design Principles:</strong>
 * <ul>
 *   <li><strong>Single Responsibility:</strong> All exception-to-response conversion happens here</li>
 *   <li><strong>Security:</strong> Internal details are NEVER exposed to clients (OWASP A05:2021)</li>
 *   <li><strong>Observability:</strong> All exceptions are logged with context for operational support</li>
 *   <li><strong>Consistency:</strong> All error responses use the {@link ErrorResponse} schema</li>
 *   <li><strong>User Experience:</strong> Client messages are human-readable and actionable</li>
 * </ul>
 *
 * <p><strong>Logging Strategy:</strong>
 * <ul>
 *   <li>WARN level: Business logic exceptions (validation, conflict, not found, auth failures)</li>
 *   <li>ERROR level: Unexpected exceptions and server-side failures (logged with full stack trace)</li>
 *   <li>DEBUG level: Client disconnections and low-priority events</li>
 *   <li>All logs include the HTTP status code for operational debugging and monitoring</li>
 * </ul>
 *
 * <p><strong>Exception-to-Status Mapping:</strong>
 * <table border="1">
 *   <tr><th>Exception Type</th><th>HTTP Status</th><th>Reason</th></tr>
 *   <tr><td>MethodArgumentNotValidException</td><td>400</td><td>DTO validation (@Valid) failed</td></tr>
 *   <tr><td>ConstraintViolationException</td><td>400</td><td>Path/query parameter constraint violation</td></tr>
 *   <tr><td>ResourceNotFoundException</td><td>404</td><td>Resource does not exist</td></tr>
 *   <tr><td>ResourceDeletedException</td><td>410</td><td>Resource was permanently deleted</td></tr>
 *   <tr><td>ConflictException</td><td>409</td><td>State conflict or duplicate key</td></tr>
 *   <tr><td>DuplicateFilterNameException</td><td>409</td><td>User tried to create duplicate filter</td></tr>
 *   <tr><td>MaxUploadSizeExceededException</td><td>413</td><td>File upload exceeds 2 GB limit</td></tr>
 *   <tr><td>IllegalArgumentException</td><td>422</td><td>Semantic validation failed</td></tr>
 *   <tr><td>UnsupportedFileTypeException</td><td>422</td><td>Unsupported MIME type or extension</td></tr>
 *   <tr><td>AuthenticationException</td><td>401</td><td>Login credentials invalid or missing JWT</td></tr>
 *   <tr><td>AuthorizationDeniedException</td><td>403</td><td>Insufficient permissions (role-based)</td></tr>
 *   <tr><td>AccessDeniedException</td><td>403</td><td>Access explicitly denied</td></tr>
 *   <tr><td>RateLimitExceededException</td><td>429</td><td>Rate limit exceeded</td></tr>
 *   <tr><td>PasswordUpdateException</td><td>500</td><td>Server-side password operation failure</td></tr>
 *   <tr><td>Exception (catch-all)</td><td>500</td><td>Unexpected error — no details exposed</td></tr>
 * </table>
 *
 * @see ErrorResponse
 * @see com.bioinformatics.common.exception
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        var violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Request validation failed: {} | HTTP 400", violations);
        return buildResponse(HttpStatus.BAD_REQUEST, violations);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        var violations = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("Path/query parameter constraint violation: {} | HTTP 400", violations);
        return buildResponse(HttpStatus.BAD_REQUEST, violations);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {} | HTTP 404", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ResourceDeletedException.class)
    public ResponseEntity<ErrorResponse> handleDeleted(ResourceDeletedException ex) {
        log.warn("Resource permanently deleted: {} | HTTP 410", ex.getMessage());
        return buildResponse(HttpStatus.GONE, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        log.warn("Request conflicts with current state: {} | HTTP 409", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFileTooLarge(MaxUploadSizeExceededException ex) {
        log.warn("File upload exceeds 2GB limit | HTTP 413");
        return buildResponse(HttpStatus.CONTENT_TOO_LARGE,
                "File exceeds maximum allowed size of 2 GB");
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    public ResponseEntity<Object> handleUnsupportedFileTypeException(UnsupportedFileTypeException ex) {
        log.warn("Unsupported file type submitted: {} | HTTP 422", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument provided: {} | HTTP 422", ex.getMessage());
        return buildResponse(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(RuntimeException ex) {
        log.warn("Authentication failed ({}): {} | HTTP 401", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        if (isClientAbort(ex)) {
            log.debug("Client disconnected before response completion: {}", ex.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        log.error("Unexpected exception caught | HTTP 500", ex);
        // Do NOT expose internal details to clients (OWASP A05:2021)
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support.");
    }

    @ExceptionHandler(DuplicateFilterNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatedFilterName(DuplicateFilterNameException ex) {
        log.warn("User attempted to create duplicate filter name: {} | HTTP 409", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "A filter with this name already exists");
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDenied(Exception ex) {
        log.warn("Authorization denied ({}): {} | HTTP 403", ex.getClass().getSimpleName(),
                ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "Access Denied");
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded: {} | HTTP 429", ex.getMessage());
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Try again later.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        var status = Objects.requireNonNullElse(HttpStatus.resolve(ex.getStatusCode().value()), HttpStatus.INTERNAL_SERVER_ERROR);
        log.warn("Response status exception: {} | HTTP {}", ex.getReason(), status.value());
        return buildResponse(status, ex.getReason());
    }

    @ExceptionHandler(PasswordUpdateException.class)
    public ResponseEntity<ErrorResponse> handlePasswordUpdateException(PasswordUpdateException ex) {
        log.error("Password update operation failed: {} | HTTP 500", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Password update failed. Please try again later.");
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
