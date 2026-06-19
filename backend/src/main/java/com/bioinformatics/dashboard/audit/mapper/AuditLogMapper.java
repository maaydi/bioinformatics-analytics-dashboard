package com.bioinformatics.dashboard.audit.mapper;

import com.bioinformatics.dashboard.audit.dto.AuditLogDto;
import com.bioinformatics.dashboard.audit.entity.AuditLog;
import com.bioinformatics.dashboard.auth.entity.AppUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {
    @Named("actorUserName")
    static String actorUserName(AppUser actor) {
        if (actor == null) return "Undefined User";
        return actor.getUsername();
    }

    @Mapping(target = "username", source = "actor", qualifiedByName = "actorUserName")
    AuditLogDto toDto(AuditLog entity);
}
