package com.bioinformatics.dashboard.audit;

import com.bioinformatics.dashboard.audit.dto.*;
import com.bioinformatics.dashboard.audit.entity.AuditLog;
import com.bioinformatics.dashboard.audit.mapper.AuditLogMapper;
import com.bioinformatics.dashboard.audit.repository.AuditLogRepository;
import com.bioinformatics.dashboard.audit.service.AuditService;
import com.bioinformatics.dashboard.auth.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditLogMapper mapper;

    @InjectMocks
    private AuditService auditService;

    @Test
    void save_shouldSaveAuditLog_whenActorIsProvided() {
        AppUser actor = new AppUser();
        actor.setId(1L);
        actor.setUsername("testuser");

        AuditWebDetails webDetails = new AuditWebDetails("GET", "/api/test", "127.0.0.1");

        auditService.save(actor, null, AuditAction.LOGIN, "target123", AuditStatus.SUCCESS, webDetails);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertThat(savedLog.getAction()).isEqualTo(AuditAction.LOGIN);
        assertThat(savedLog.getTarget()).isEqualTo(AuditTarget.AUTH);
        assertThat(savedLog.getTargetId()).isEqualTo("target123");
        assertThat(savedLog.getStatus()).isEqualTo(AuditStatus.SUCCESS);
        assertThat(savedLog.getActorId()).isEqualTo(1L);
        assertThat(savedLog.getActorUsername()).isEqualTo("testuser");
        assertThat(savedLog.getHttpMethod()).isEqualTo("GET");
        assertThat(savedLog.getEndpoint()).isEqualTo("/api/test");
        assertThat(savedLog.getIpAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    void save_shouldSaveAuditLog_whenActorIsNull() {
        auditService.save(null, "attemptedUser", AuditAction.LOGIN, null, AuditStatus.FAILURE, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertThat(savedLog.getActorId()).isNull();
        assertThat(savedLog.getActorUsername()).isEqualTo("attemptedUser");
        assertThat(savedLog.getHttpMethod()).isEqualTo("SYSTEM");
        assertThat(savedLog.getEndpoint()).isEqualTo("INTERNAL");
        assertThat(savedLog.getIpAddress()).isNull();
    }

    @Test
    void findByUserId_shouldReturnMappedDtoPage() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        AuditLog log = new AuditLog();
        Page<AuditLog> page = new PageImpl<>(List.of(log));

        when(auditLogRepository.findByActorId(1L, pageRequest)).thenReturn(page);
        when(mapper.toDto(log)).thenReturn(new AuditLogDto(1L, 1L, "testuser", AuditAction.LOGIN, AuditTarget.AUTH, null, AuditStatus.SUCCESS, null, "GET", "/api/test", Instant.now()));

        Page<AuditLogDto> result = auditService.findByUserId(1L, pageRequest);

        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository).findByActorId(1L, pageRequest);
        verify(mapper).toDto(log);
    }
}

