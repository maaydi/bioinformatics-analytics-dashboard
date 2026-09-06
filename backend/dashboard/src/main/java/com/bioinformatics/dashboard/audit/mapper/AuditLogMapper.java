package com.bioinformatics.dashboard.audit.mapper;

import com.bioinformatics.dashboard.audit.dto.AuditLogDto;
import com.bioinformatics.dashboard.audit.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(target = "username", source = "actorUsername")
    /**
     * Map an AuditLog JPA entity to its DTO projection.
     *
     * @param entity the AuditLog entity
     * @return the AuditLogDto projection
     */
    AuditLogDto toDto(AuditLog entity);
}
