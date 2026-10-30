package com.bioinformatics.auditservice.repository;

import com.bioinformatics.auditservice.dto.AuditSearchRequest;
import com.bioinformatics.auditservice.entity.AuditLog;
import com.bioinformatics.auditservice.service.AuditService;
import com.bioinformatics.auditservice.specification.AuditSearchSpecification;
import com.bioinformatics.auditservice.support.AuditIntegrationTest;
import com.bioinformatics.shared.models.audit.AuditAction;
import com.bioinformatics.shared.models.audit.AuditStatus;
import com.bioinformatics.shared.models.audit.AuditTarget;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.bioinformatics.auditservice.support.AuditTestFixtures.CREATED_AT;
import static com.bioinformatics.auditservice.support.AuditTestFixtures.event;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class AuditLogRepositoryIntegrationTest extends AuditIntegrationTest {

    @Autowired
    private AuditLogRepository repository;
    @Autowired
    private AuditService service;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void insertSearchFixtures() {
        insert(10001, "alice", "USER", "auth-service", "request-a", AuditAction.LOGIN,
                AuditStatus.SUCCESS, "192.0.2.10", """
                        {"environment":"test","attempts":2,"allowed":true}
                        """, CREATED_AT);
        insert(10002, "bob", "SYSTEM", "import-service", "request-b", AuditAction.IMPORT_UPLOAD,
                AuditStatus.FAILURE, "2001:db8::1", """
                        {"environment":"prod","attempts":5,"allowed":false}
                        """, CREATED_AT.plusSeconds(60));
        insert(10003, "alice", "USER", "audit-service", "request-c", AuditAction.AUDIT_SEARCH,
                AuditStatus.SUCCESS, "192.0.2.11", """
                        {"environment":"test","attempts":3}
                        """, CREATED_AT.plusSeconds(120));
        insert(10004, "charlie", "SERVICE", "auth-service", "request-a", AuditAction.LOGOUT,
                AuditStatus.FAILURE, "192.0.2.12", "{}", CREATED_AT.plusSeconds(180));
    }

    @ParameterizedTest
    @MethodSource("searchFilters")
    void filtersAgainstPostgresTypes(AuditSearchRequest request, List<Long> expectedIds) {
        var matches = repository.findAll(AuditSearchSpecification.fromRequest(request), Sort.by("id"));

        assertThat(matches).extracting(AuditLog::getId).containsExactlyElementsOf(expectedIds);
    }

    static Stream<Arguments> searchFilters() {
        var builder = AuditSearchRequest.builder();
        return Stream.of(
                Arguments.of(builder.build(), List.of(10001L, 10002L, 10003L, 10004L)),
                Arguments.of(builder.actorIdentities(List.of("alice", "bob")).build(),
                        List.of(10001L, 10002L, 10003L)),
                Arguments.of(AuditSearchRequest.builder().actorTypes(List.of("SYSTEM")).build(), List.of(10002L)),
                Arguments.of(AuditSearchRequest.builder().sourceServices(List.of("auth-service")).build(),
                        List.of(10001L, 10004L)),
                Arguments.of(AuditSearchRequest.builder().correlationIds(List.of("request-a")).build(),
                        List.of(10001L, 10004L)),
                Arguments.of(AuditSearchRequest.builder().actions(List.of(AuditAction.AUDIT_SEARCH)).build(),
                        List.of(10003L)),
                Arguments.of(AuditSearchRequest.builder().targets(List.of(AuditTarget.AUTH)).build(),
                        List.of(10001L, 10004L)),
                Arguments.of(AuditSearchRequest.builder().statuses(List.of(AuditStatus.FAILURE)).build(),
                        List.of(10002L, 10004L)),
                Arguments.of(AuditSearchRequest.builder().actorIdentities(List.of("nobody")).build(), List.of()),
                Arguments.of(AuditSearchRequest.builder().ipAddresses(List.of("192.0.2.10")).build(), List.of(10001L)),
                Arguments.of(AuditSearchRequest.builder().ipAddresses(List.of("2001:db8::1")).build(), List.of(10002L)),
                Arguments.of(AuditSearchRequest.builder().ipAddresses(List.of("192.0.2.999")).build(), List.of()),
                Arguments.of(AuditSearchRequest.builder().ipAddresses(List.of("192.0.2.999", "192.0.2.11")).build(),
                        List.of(10003L)),
                Arguments.of(AuditSearchRequest.builder().metadata(Map.of("environment", List.of("test"))).build(),
                        List.of(10001L, 10003L)),
                Arguments.of(AuditSearchRequest.builder().metadata(Map.of("attempts", List.of(2, 5))).build(),
                        List.of(10001L, 10002L)),
                Arguments.of(AuditSearchRequest.builder().metadata(Map.of("allowed", List.of(true))).build(),
                        List.of(10001L)),
                Arguments.of(AuditSearchRequest.builder().metadata(Map.of("missing", List.of("value"))).build(),
                        List.of()),
                Arguments.of(AuditSearchRequest.builder().metadata(Map.of("environment", List.of("test"),
                        "attempts", List.of(3))).build(), List.of(10003L))
        );
    }

    @Test
    void combinesAllFiltersWithAnd() {
        var request = AuditSearchRequest.builder()
                .actorIdentities(List.of("alice"))
                .actorTypes(List.of("USER"))
                .sourceServices(List.of("auth-service"))
                .correlationIds(List.of("request-a"))
                .actions(List.of(AuditAction.LOGIN))
                .targets(List.of(AuditTarget.AUTH))
                .statuses(List.of(AuditStatus.SUCCESS))
                .ipAddresses(List.of("192.0.2.10"))
                .metadata(Map.of("attempts", List.of(2), "allowed", List.of(true)))
                .startDate(CREATED_AT.minusSeconds(1)).endDate(CREATED_AT.plusSeconds(1)).build();

        assertThat(repository.findAll(AuditSearchSpecification.fromRequest(request)))
                .extracting(AuditLog::getId).containsExactly(10001L);
    }

    @Test
    void emptyFiltersAndNullRequestsDoNotRestrictResults() {
        Map<String, List<Object>> ignoredMetadata = new HashMap<>();
        ignoredMetadata.put("empty", List.of());
        ignoredMetadata.put("absent", null);
        var request = AuditSearchRequest.builder()
                .actorIdentities(List.of()).actorTypes(List.of()).sourceServices(List.of())
                .correlationIds(List.of()).actions(List.of()).targets(List.of())
                .statuses(List.of()).ipAddresses(List.of()).metadata(ignoredMetadata).build();

        assertThat(repository.count(AuditSearchSpecification.fromRequest(null))).isEqualTo(4);
        assertThat(repository.count(AuditSearchSpecification.fromRequest(request))).isEqualTo(4);
        assertThat(AuditSearchSpecification.metadata(Map.of())).isNull();
    }

    @Test
    void dateRangeIncludesBothBoundaries() {
        var spec = AuditSearchSpecification.createdAt(CREATED_AT, CREATED_AT.plusSeconds(120));

        assertThat(repository.findAll(spec, Sort.by("id")))
                .extracting(AuditLog::getId).containsExactly(10001L, 10002L, 10003L);
    }

    @Test
    void startOnlyDateFilterExcludesItsBoundary() {
        var spec = AuditSearchSpecification.createdAt(CREATED_AT.plusSeconds(60), null);

        assertThat(repository.findAll(spec, Sort.by("id")))
                .extracting(AuditLog::getId).containsExactly(10003L, 10004L);
    }

    @Test
    void endOnlyDateFilterExcludesItsBoundary() {
        var spec = AuditSearchSpecification.createdAt(null, CREATED_AT.plusSeconds(120));

        assertThat(repository.findAll(spec, Sort.by("id")))
                .extracting(AuditLog::getId).containsExactly(10001L, 10002L);
    }

    @Test
    void actorLookupUsesPaginationAndExactIdentity() {
        var page = repository.findByActorIdentity("alice", PageRequest.of(1, 1, Sort.by("createdAt").descending()));

        assertThat(page.getContent()).extracting(AuditLog::getId).containsExactly(10001L);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(repository.findByActorIdentity("ali", PageRequest.of(0, 20))).isEmpty();
    }

    @Test
    void persistsAnEventThroughTheRealMapperAndRepository() {
        var event = event("attempts", 2, "allowed", true, "nested", Map.of("name", "value"));
        var before = Instant.now();

        var saved = service.save(event);
        repository.flush();
        entityManager.clear();
        var reloaded = repository.findById(saved.id()).orElseThrow();

        assertThat(saved.id()).isPositive();
        assertThat(saved.eventId()).isEqualTo(event.eventId());
        assertThat(saved.actorIdentity()).isEqualTo(event.actorIdentity());
        assertThat(saved.actorType()).isEqualTo(event.actorType());
        assertThat(saved.sourceService()).isEqualTo(event.sourceService());
        assertThat(saved.correlationId()).isEqualTo(event.correlationId());
        assertThat(saved.action()).isEqualTo(event.action());
        assertThat(saved.targetType()).isEqualTo(event.action().getDefaultTarget());
        assertThat(saved.targetId()).isEqualTo(event.targetId());
        assertThat(saved.status()).isEqualTo(event.status());
        assertThat(saved.ipAddress()).isEqualTo(event.ipAddress());
        assertThat(saved.metadata()).containsExactlyInAnyOrderEntriesOf(
                Map.of("attempts", 2, "allowed", true, "nested", Map.of("name", "value")));
        assertThat(saved.createdAt()).isBetween(before, Instant.now());
        assertThat(reloaded.getEventId()).isEqualTo(event.eventId());
        assertThat(reloaded.getMetadata()).isEqualTo(saved.metadata());
        assertThat(reloaded.getIpAddress().getHostAddress()).isEqualTo(event.ipAddress());
        assertThat(jdbc.queryForObject("SELECT metadata ->> 'attempts' FROM audit_log.audit_event WHERE id = ?",
                String.class, saved.id())).isEqualTo("2");
        assertThat(jdbc.queryForObject("SELECT host(ip_address) FROM audit_log.audit_event WHERE id = ?",
                String.class, saved.id())).isEqualTo(event.ipAddress());
    }

    @Test
    void rejectsDuplicateEventIdsAtTheDatabaseBoundary() {
        var event = event();
        service.save(event);
        repository.flush();

        assertThatThrownBy(() -> {
            service.save(event);
            repository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private void insert(long id, String actor, String actorType, String source, String correlation,
                        AuditAction action, AuditStatus status, String ip, String metadata, Instant createdAt) {
        jdbc.update("""
                INSERT INTO audit_log.audit_event
                    (id, event_id, actor_identity, actor_type, source_service, correlation_id,
                     action, target_type, target_id, status, ip_address, metadata, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::inet, ?::jsonb, ?)
                """, id, UUID.randomUUID(), actor, actorType, source, correlation, action.name(),
                action.getDefaultTarget().name(), "target-" + id, status.name(), ip, metadata,
                java.sql.Timestamp.from(createdAt));
    }
}
