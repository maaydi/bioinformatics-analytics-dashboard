package com.bioinformatics.auditservice.mapper;

import com.bioinformatics.auditservice.dto.AuditLogDto;
import com.bioinformatics.auditservice.entity.AuditLog;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Mapper(componentModel = "spring")
@DecoratedWith(AuditLogMapperDecorator.class)
public interface AuditLogMapper {

    /**
     * Map an AuditLog JPA entity to its DTO projection.
     *
     * @param entity the AuditLog entity
     * @return the AuditLogDto projection
     */
    @Mapping(target = "ipAddress", expression = "java(entity.getIpAddress() != null ? entity.getIpAddress().getHostAddress() : null)")
    AuditLogDto toDto(AuditLog entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ipAddress", source = "ipAddress", qualifiedByName = "strToInetAddress")
    AuditLog toEntity(AuditLogDto dto);


    @Named("strToInetAddress")
    static InetAddress convertStringToInetAddress(String host) throws UnknownHostException {
        return InetAddress.getByName(host);
    }
}
