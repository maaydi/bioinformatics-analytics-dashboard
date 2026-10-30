package com.bioinformatics.auditservice.service;

import com.bioinformatics.auditservice.dto.AuditLogDto;
import com.bioinformatics.auditservice.dto.AuditSearchRequest;
import com.bioinformatics.auditservice.entity.AuditLog;
import com.bioinformatics.auditservice.mapper.AuditLogMapper;
import com.bioinformatics.auditservice.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;

import static com.bioinformatics.auditservice.support.AuditTestFixtures.event;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository repository;
    @Mock
    private AuditLogMapper mapper;
    @InjectMocks
    private AuditService service;

    @Test
    void savesEveryEventFieldAndReturnsThePersistedProjection() {
        var event = event("endpoint", "/login", "attempts", 2, "allowed", true);
        var entity = new AuditLog();
        var saved = new AuditLog();
        var expected = new AuditLogDto(42L, event.eventId(), event.actorIdentity(), event.actorType(),
                event.sourceService(), event.correlationId(), event.action(), event.targetType(),
                event.targetId(), event.status(), event.ipAddress(),
                Map.of("endpoint", "/login", "attempts", 2, "allowed", true), event.createdAt());
        when(mapper.toEntity(any(AuditLogDto.class))).thenReturn(entity);
        when(repository.save(entity)).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(expected);

        assertThat(service.save(event)).isSameAs(expected);

        var dto = ArgumentCaptor.forClass(AuditLogDto.class);
        var order = inOrder(mapper, repository);
        order.verify(mapper).toEntity(dto.capture());
        order.verify(repository).save(entity);
        order.verify(mapper).toDto(saved);
        assertThat(dto.getValue()).isEqualTo(new AuditLogDto(0L, event.eventId(), event.actorIdentity(),
                event.actorType(), event.sourceService(), event.correlationId(), event.action(),
                event.targetType(), event.targetId(), event.status(), event.ipAddress(),
                expected.metadata(), event.createdAt()));
    }

    @Test
    void acceptsAnEventWithoutMetadata() {
        service.save(event());

        var dto = ArgumentCaptor.forClass(AuditLogDto.class);
        verify(mapper).toEntity(dto.capture());
        assertThat(dto.getValue().metadata()).isEmpty();
    }

    @Test
    void keepsTheLastValueForRepeatedMetadataKeysAndPreservesNullValues() {
        service.save(event("key", "first", "key", "last", "optional", null));

        var dto = ArgumentCaptor.forClass(AuditLogDto.class);
        verify(mapper).toEntity(dto.capture());
        assertThat(dto.getValue().metadata()).containsEntry("key", "last").containsEntry("optional", null);
    }

    @Test
    void rejectsUnpairedMetadataBeforeMappingOrSaving() {
        assertThatThrownBy(() -> service.save(event("unpaired")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Metadata should be a pair of key, value");

        verifyNoInteractions(mapper, repository);
    }

    @Test
    void propagatesMappingFailuresWithoutSaving() {
        var failure = new IllegalArgumentException("Invalid address");
        when(mapper.toEntity(any(AuditLogDto.class))).thenThrow(failure);

        assertThatThrownBy(() -> service.save(event())).isSameAs(failure);

        verifyNoInteractions(repository);
    }

    @Test
    void propagatesPersistenceFailuresWithoutReturningAProjection() {
        var entity = new AuditLog();
        var failure = new DataAccessResourceFailureException("Database unavailable");
        when(mapper.toEntity(any(AuditLogDto.class))).thenReturn(entity);
        when(repository.save(entity)).thenThrow(failure);

        assertThatThrownBy(() -> service.save(event())).isSameAs(failure);

        verify(mapper, never()).toDto(any());
    }

    @Test
    void mapsSearchResultsAndPreservesPaginationAndSorting() {
        var request = AuditSearchRequest.builder().actorIdentities(List.of("alice"))
                .page(1).size(2).sort("actorIdentity").direction("desc").build();
        var entity = new AuditLog();
        var dto = new AuditLogDto(42L, null, "alice", null, null, null,
                null, null, null, null, null, Map.of(), null);
        when(repository.findAll(ArgumentMatchers.<Specification<AuditLog>>any(), eq(request.getRequestPage())))
                .thenReturn(new PageImpl<>(List.of(entity), request.getRequestPage(), 5));
        when(mapper.toDto(entity)).thenReturn(dto);

        var result = service.listAuditLogs(request);

        assertThat(result.content()).containsExactly(dto);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
    }

    @Test
    void returnsAnEmptyPageWithoutInvokingTheMapper() {
        var request = AuditSearchRequest.builder().page(0).size(20).build();
        when(repository.findAll(ArgumentMatchers.<Specification<AuditLog>>any(), eq(request.getRequestPage())))
                .thenReturn(Page.empty(request.getRequestPage()));

        var result = service.listAuditLogs(request);

        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
        verifyNoInteractions(mapper);
    }
}
