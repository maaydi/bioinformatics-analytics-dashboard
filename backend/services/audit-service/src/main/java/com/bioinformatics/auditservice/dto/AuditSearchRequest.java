package com.bioinformatics.auditservice.dto;

import com.bioinformatics.shared.models.audit.AuditAction;
import com.bioinformatics.shared.models.audit.AuditStatus;
import com.bioinformatics.shared.models.audit.AuditTarget;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.bioinformatics.common.models.SearchRequestUtils.*;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Builder
public record AuditSearchRequest(

        List<String> actorIdentities,
        List<String> actorTypes,
        List<String> sourceServices,
        List<String> correlationIds,
        List<AuditAction> actions,
        List<AuditTarget> targets,
        List<AuditStatus> statuses,
        List<String> ipAddresses,
        Map<String, List<Object>> metadata,
        Instant startDate,
        Instant endDate,
        @Min(value = 0, message = "page must be ≥ 0")
        Integer page,

        @Min(1) @Max(200)
        Integer size,

        String sort,

        @Pattern(regexp = "asc|desc")
        String direction
) {


    /**
     * Validates that start date before end date.
     *
     */
    @AssertTrue(message = "Start date should be before End Date")
    private boolean isDateRangeValid() {
        if (startDate() == null || endDate() == null) return true;
        return startDate().isBefore(endDate());
    }

    /**
     * Creates the pageable definition for this search request.
     *
     * @return pageable containing the requested page, size, sort field, and direction
     * @throws IllegalArgumentException if the requested sort field or direction is invalid
     */
    public Pageable getRequestPage() {
        var dir = direction == null ? "asc" : direction;
        var direct = Sort.Direction.fromString(dir);
        var sortField = sort == null ? "createdAt" : sort;
        return PageRequest.of(page, size, direct, sortField);
    }

    /**
     * Returns a diagnostic representation containing populated search and pagination fields.
     *
     * @return a string representation of this request
     */
    @Override
    public @NonNull String toString() {
        return "AuditSearchRequest[" + fieldsToString() +
                ',' +
                PaginationToString(page(),size(),sort(),direction()) +
                "]";
    }

    /**
     * Formats the populated search-filter fields for diagnostic output.
     *
     * @return formatted search-field text
     */
    private String fieldsToString() {
        String sb = "Fields=[" + fieldToString("actorIdentities", actorIdentities) +
                fieldToString("actorTypes", actorTypes) +
                fieldToString("sourceServices", sourceServices) +
                fieldToString("correlationIds", correlationIds) +
                fieldToString("actions", actions) +
                fieldToString("targets", targets) +
                fieldToString("statuses", statuses) +
                fieldToString("ipAddresses", ipAddresses) +
                fieldToString("startDate", startDate) +
                fieldToString("endDate", endDate) +
                metadataToString();
        var s = sb.trim();
        if (s.endsWith(",")) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "]";
    }

    private String metadataToString() {
        if (metadata == null) return "";
        return metadata.entrySet().stream()
                .map(e -> fieldToString(e.getKey(), e.getValue()))
                .collect(Collectors.joining(""));
    }

    /**
     * Creates a builder initialized with this request's values.
     *
     * @return a mutable builder pre-populated from this request
     */
    public AuditSearchRequest.AuditSearchRequestBuilder copy() {
        return AuditSearchRequest.builder()
                .actorIdentities(safeListCopy( actorIdentities))
                .actorTypes(safeListCopy(actorTypes))
                .sourceServices(safeListCopy(sourceServices))
                .correlationIds(safeListCopy(correlationIds))
                .actions(safeListCopy(actions))
                .targets(safeListCopy(targets))
                .statuses(safeListCopy(statuses))
                .ipAddresses(safeListCopy(ipAddresses))
                .metadata(safeMapCopy(metadata))
                .startDate(startDate)
                .endDate(endDate)
                .page(page)
                .size(size)
                .sort(sort)
                .direction(direction);
    }



    /**
     * Compares this request with another request by value.
     *
     * <p>List-valued filters are compared as sets: element order and duplicate occurrences do
     * not affect equality.
     *
     * @param o object to compare with this request
     * @return {@code true} when both requests contain equivalent values
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuditSearchRequest(
                List<String> identities, List<String> types, List<String> services, List<String> ids,
                List<AuditAction> actions1, List<AuditTarget> targets1, List<AuditStatus> statuses1,
                List<String> addresses, Map<String, List<Object>> metadata1, Instant date, Instant endDate1,
                Integer page1, Integer size1, String sort1, String direction1
        ))) return false;
        return listEquals(actorIdentities, identities)
                && listEquals(actorTypes, types)
                && listEquals(sourceServices, services)
                && listEquals(correlationIds, ids)
                && listEquals(actions, actions1)
                && listEquals(targets, targets1)
                && listEquals(statuses, statuses1)
                && listEquals(ipAddresses, addresses)
                && metadataEquals(metadata, metadata1)
                && Objects.equals(startDate, date)
                && Objects.equals(endDate, endDate1)
                && Objects.equals(page, page1)
                && Objects.equals(size, size1)
                && Objects.equals(sort, sort1)
                && Objects.equals(direction, direction1);
    }

    /**
     * Returns the hash code for this request.
     *
     * @return a hash code consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                listHashCode(actorIdentities),
                listHashCode(actorTypes),
                listHashCode(sourceServices),
                listHashCode(correlationIds),
                listHashCode(actions),
                listHashCode(targets),
                listHashCode(statuses),
                listHashCode(ipAddresses),
                metadataHashCode(metadata),
                startDate,
                endDate,
                page,
                size,
                sort,
                direction
        );
    }

    private static boolean metadataEquals(
            Map<String, List<Object>> first,
            Map<String, List<Object>> second
    ) {
        if (first == second) return true;
        if (first == null || second == null || first.size() != second.size()) return false;
        for (var entry : first.entrySet()) {
            if (!second.containsKey(entry.getKey())
                    || !listEquals(entry.getValue(), second.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private static int listHashCode(List<?> values) {
        return values == null ? 0 : new HashSet<>(values).hashCode();
    }

    private static int metadataHashCode(Map<String, List<Object>> values) {
        if (values == null) return 0;
        var hashCode = 0;
        for (var entry : values.entrySet()) {
            hashCode += Objects.hashCode(entry.getKey()) ^ listHashCode(entry.getValue());
        }
        return hashCode;
    }
}