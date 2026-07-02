package com.bioinformatics.dashboard.audit.mapper;

import com.bioinformatics.dashboard.audit.entity.AuditLog;
import com.bioinformatics.dashboard.model.audit.AuditLogDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(target = "username", source = "actorUsername")
    @Mapping(target = "userId", source = "actorId")
    /**
     * Map an AuditLog JPA entity to its DTO projection.
     *
     * @param entity the AuditLog entity
     * @return the AuditLogDto projection
     */
    AuditLogDto toDto(AuditLog entity);
}
