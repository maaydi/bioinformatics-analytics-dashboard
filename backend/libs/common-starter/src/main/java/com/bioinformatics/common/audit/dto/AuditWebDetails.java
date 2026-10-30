package com.bioinformatics.common.audit.dto;

/**
 * Web request details captured for audit logging.
 *
 * @param httpMethod the HTTP method (e.g., GET, POST)
 * @param endpoint   the request URI
 * @param ipAddress  the client IP address
 * @param correlationId incoming correlation identifier, if supplied
 */
public record AuditWebDetails(String httpMethod, String endpoint, String ipAddress, String correlationId) {
}
