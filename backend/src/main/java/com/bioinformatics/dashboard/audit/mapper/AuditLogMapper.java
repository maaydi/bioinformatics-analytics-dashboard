package com.bioinformatics.dashboard.audit.mapper;

import com.bioinformatics.dashboard.audit.dto.AuditLogDto;
import com.bioinformatics.dashboard.audit.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(target = "username", source = "actorUsername")
    @Mapping(target = "userId", source = "actorId")
    AuditLogDto toDto(AuditLog entity);
}
