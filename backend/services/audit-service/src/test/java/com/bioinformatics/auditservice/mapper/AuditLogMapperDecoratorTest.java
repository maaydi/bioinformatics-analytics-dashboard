package com.bioinformatics.auditservice.mapper;

import com.bioinformatics.auditservice.dto.AuditLogDto;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogMapperDecoratorTest {

    @Test
    void injectsQualifiedDelegateAndMapsInBothDirections() {
        try (var context = new AnnotationConfigApplicationContext(
                AuditLogMapper.class.getPackageName())) {
            var mapper = context.getBean(AuditLogMapper.class);
            var dto = new AuditLogDto(
                    0L, UUID.randomUUID(), "admin", "USER", "audit-service",
                    "correlation-id", null, null, "target-id", null,
                    "127.0.0.1", Map.of("key", "value"), Instant.parse("2026-01-01T00:00:00Z"));

            assertThat(mapper).isInstanceOf(AuditLogMapperDecorator.class);

            var entity = mapper.toEntity(dto);

            assertThat(entity.getId()).isNull();
            assertThat(entity.getIpAddress().getHostAddress()).isEqualTo(dto.ipAddress());

            entity.setId(dto.id());
            assertThat(mapper.toDto(entity)).isEqualTo(dto);
        }
    }
}
