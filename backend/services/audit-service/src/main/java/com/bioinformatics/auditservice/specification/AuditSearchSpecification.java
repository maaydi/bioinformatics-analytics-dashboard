package com.bioinformatics.auditservice.specification;

import com.bioinformatics.auditservice.dto.AuditSearchRequest;
import com.bioinformatics.auditservice.entity.AuditLog;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * JPA specifications for dynamic filtering on {@link com.bioinformatics.auditservice.entity.AuditLog}.
 *
 * <p>Each factory method builds a single predicate, and the service layer combines them into a single
 * query. This keeps the domain query logic explicit, testable, and aligned with the filters exposed by
 * the genes search API.
 */
@Slf4j
public final class AuditSearchSpecification {

    private AuditSearchSpecification() {
    }

    public static Specification<AuditLog> fromRequest(AuditSearchRequest req) {
        if (Objects.isNull(req)) {
            return (root, query, cb) -> cb.conjunction();
        }

        var filters = Stream.of(
                getInClause("actorIdentity", req.actorIdentities()),
                getInClause("actorType", req.actorTypes()),
                getInClause("sourceService", req.sourceServices()),
                getInClause("correlationId", req.correlationIds()),
                getInClause("action", req.actions()),
                getInClause("targetType", req.targets()),
                getInClause("status", req.statuses()),
                ipAddress(req.ipAddresses()),
                metadata(req.metadata()),
                createdAt(req.startDate(), req.endDate())
        ).toList();

        var specification = filters.stream().filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());

        log.debug("Built AuditSearchSpecification from request with {} active filters", filters.stream().filter(Objects::nonNull).count());
        return specification;
    }

    public static <T> Specification<AuditLog> getInClause(String column, List<T> items) {
        if (items == null || items.isEmpty()) return null;
        return (root, cq, cb) -> root.get(column).in(items);
    }

    public static Specification<AuditLog> ipAddress(List<String> hosts) {
        if (hosts == null || hosts.isEmpty()) return null;
        var items = hosts.stream().map(e -> {
                    try {
                        return InetAddress.getByName(e);
                    } catch (UnknownHostException ex) {
                        return null;
                    }
                }).filter(Objects::nonNull)
                .toList();
        return (root, cq, cb) -> root.get("ipAddress").in(items);
    }

    public static Specification<AuditLog> metadata(Map<String, List<Object>> metadata) {
        if (metadata == null || metadata.isEmpty()) return null;
        return (root, cq, cb) -> {
            var predicates = metadata.entrySet().stream()
                    .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                    .map(entry -> cb.function(
                            "jsonb_extract_path_text",
                            String.class,
                            root.get("metadata"),
                            cb.literal(entry.getKey())
                    ).in(entry.getValue().stream()
                            .map(value -> Objects.toString(value, null))
                            .toList()))
                    .toArray(Predicate[]::new);
            return cb.and(predicates);
        };
    }

    public static Specification<AuditLog> createdAt(Instant start, Instant end) {
        if (start == null && end == null) return null;
        return (root, cq, cb) -> {
            var createdAt = root.<Instant>get("createdAt");
            if (start != null && end != null) {
                return cb.between(createdAt, start, end);
            }
            if (start != null) {
                return cb.greaterThan(createdAt, start);
            }
            return cb.lessThan(createdAt, end);
        };
    }

}
