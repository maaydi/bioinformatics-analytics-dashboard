package com.bioinformatics.dashboard.audit.dto;

/**
 * Web request details captured for audit logging.
 *
 * @param httpMethod the HTTP method (e.g., GET, POST)
 * @param endpoint   the request URI
 * @param ipAddress  the client IP address
 */
public record AuditWebDetails(String httpMethod, String endpoint, String ipAddress) {
}
