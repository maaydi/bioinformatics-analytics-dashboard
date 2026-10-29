package com.bioinformatics.auditservice.mapper;

import com.bioinformatics.auditservice.dto.AuditLogDto;
import com.bioinformatics.auditservice.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    /**
     * Map an AuditLog JPA entity to its DTO projection.
     *
     * @param entity the AuditLog entity
     * @return the AuditLogDto projection
     */
    @Mapping(target = "ipAddress", expression = "java(entity.getIpAddress() != null ? entity.getIpAddress().getHostAddress() : null)")
    AuditLogDto toDto(AuditLog entity);
}
