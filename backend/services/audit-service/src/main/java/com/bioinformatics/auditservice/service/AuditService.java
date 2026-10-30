package com.bioinformatics.auditservice.service;

import com.bioinformatics.auditservice.dto.AuditLogDto;
import com.bioinformatics.auditservice.dto.AuditSearchRequest;
import com.bioinformatics.auditservice.mapper.AuditLogMapper;
import com.bioinformatics.auditservice.repository.AuditLogRepository;
import com.bioinformatics.auditservice.specification.AuditSearchSpecification;
import com.bioinformatics.common.models.PagedResponse;
import com.bioinformatics.shared.models.kafka.events.AuditLogEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository repository;
    private final AuditLogMapper mapper;

    public AuditLogDto save(AuditLogEvent event) {
        var dto = new AuditLogDto(0L,
                event.eventId(),
                event.actorIdentity(),
                event.actorType(),
                event.sourceService(),
                event.correlationId(),
                event.action(),
                event.targetType(),
                event.targetId(),
                event.status(),
                event.ipAddress(),
                toMapMetadata(event.metadata()),
                event.createdAt());

        var entity = mapper.toEntity(dto);
        var saved = repository.save(entity);
        return mapper.toDto(saved);
    }

    private static Map<String, Object> toMapMetadata(Object... metadata) {
        if (metadata.length % 2 != 0) {
            throw new IllegalArgumentException("Metadata should be a pair of key, value");
        }
        var m = new HashMap<String, Object>();
        for (var i = 0; i < metadata.length; i += 2) {
            m.put((String) metadata[i], metadata[i + 1]);
        }
        return m;
    }

    public PagedResponse<AuditLogDto> listAuditLogs(@Valid AuditSearchRequest request) {
        var page = request.getRequestPage();
        var spec = AuditSearchSpecification.fromRequest(request);
        var result = repository.findAll(spec, page).map(mapper::toDto);
        return PagedResponse.of(result);
    }
}
