package com.bioinformatics.auditservice.mapper;

import com.bioinformatics.auditservice.dto.AuditLogDto;
import com.bioinformatics.auditservice.entity.AuditLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@Slf4j
public abstract class AuditLogMapperDecorator implements AuditLogMapper {

    private AuditLogMapper delegate;

    @Autowired
    public void setDelegate(@Qualifier("delegate") AuditLogMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public AuditLogDto toDto(AuditLog entity) {
        try {
            return delegate.toDto(entity);
        } catch (Exception e) {
            log.error("Failed to convert Entity to DTO {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public AuditLog toEntity(AuditLogDto dto) {
        try {
            return delegate.toEntity(dto);
        } catch (Exception e) {
            log.error("Failed to convert DTO to Entity {}", e.getMessage());
            throw e;
        }
    }
}
