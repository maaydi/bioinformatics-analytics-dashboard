package com.bioinformatics.dashboard.audit.dto;

public record AuditWebDetails(String httpMethod, String endpoint, String ipAddress) {
}
