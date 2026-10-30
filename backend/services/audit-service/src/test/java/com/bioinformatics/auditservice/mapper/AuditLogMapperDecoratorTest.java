package com.bioinformatics.auditservice.mapper;

import com.bioinformatics.auditservice.dto.AuditLogDto;
import com.bioinformatics.auditservice.entity.AuditLog;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void mapsNullInputsAndMissingEntityAddresses() {
        try (var context = new AnnotationConfigApplicationContext(AuditLogMapper.class.getPackageName())) {
            var mapper = context.getBean(AuditLogMapper.class);

            assertThat(mapper.toDto(null)).isNull();
            assertThat(mapper.toEntity(null)).isNull();
            assertThat(mapper.toDto(new AuditLog()).ipAddress()).isNull();
            var dto = new AuditLogDto(0L, null, null, null, "audit-service",
                    null, null, null, null, null, "192.0.2.10", null, Instant.EPOCH);
            assertThat(mapper.toEntity(dto).getMetadata()).isNull();
        }
    }

    @Test
    void roundTripsIpv6Addresses() {
        try (var context = new AnnotationConfigApplicationContext(AuditLogMapper.class.getPackageName())) {
            var mapper = context.getBean(AuditLogMapper.class);
            var dto = new AuditLogDto(0L, UUID.randomUUID(), null, null, "audit-service",
                    null, null, null, null, null, "2001:db8::1", Map.of(), Instant.EPOCH);

            var entity = mapper.toEntity(dto);

            assertThat(entity.getIpAddress().getAddress()).hasSize(16);
            assertThat(mapper.toDto(entity).ipAddress()).isEqualTo("2001:db8:0:0:0:0:0:1");
        }
    }

    @Test
    void invalidAddressesAreNotSilentlyDiscarded() {
        try (var context = new AnnotationConfigApplicationContext(AuditLogMapper.class.getPackageName())) {
            var mapper = context.getBean(AuditLogMapper.class);
            var dto = new AuditLogDto(0L, UUID.randomUUID(), null, null, "audit-service",
                    null, null, null, null, null, "192.0.2.999", Map.of(), Instant.EPOCH);

            assertThatThrownBy(() -> mapper.toEntity(dto)).hasRootCauseInstanceOf(UnknownHostException.class);
        }
    }

    @Test
    void preservesTheOriginalEntityMappingFailure() {
        var delegate = mock(AuditLogMapper.class);
        var decorator = new AuditLogMapperDecorator() {};
        decorator.setDelegate(delegate);
        var entity = new AuditLog();
        var failure = new IllegalStateException("Entity mapping failed");
        when(delegate.toDto(entity)).thenThrow(failure);

        assertThatThrownBy(() -> decorator.toDto(entity)).isSameAs(failure);
    }

    @Test
    void preservesTheOriginalDtoMappingFailure() {
        var delegate = mock(AuditLogMapper.class);
        var decorator = new AuditLogMapperDecorator() {};
        decorator.setDelegate(delegate);
        var dto = new AuditLogDto(0L, null, null, null, null, null,
                null, null, null, null, null, null, null);
        var failure = new IllegalStateException("DTO mapping failed");
        when(delegate.toEntity(dto)).thenThrow(failure);

        assertThatThrownBy(() -> decorator.toEntity(dto)).isSameAs(failure);
    }
}
