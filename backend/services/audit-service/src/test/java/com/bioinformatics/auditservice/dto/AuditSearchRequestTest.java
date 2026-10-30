package com.bioinformatics.auditservice.dto;

import com.bioinformatics.shared.models.audit.AuditAction;
import com.bioinformatics.shared.models.audit.AuditStatus;
import com.bioinformatics.shared.models.audit.AuditTarget;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.bioinformatics.auditservice.support.AuditTestFixtures.CREATED_AT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditSearchRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void defaultsToAscendingCreationTimeSort() {
        var page = AuditSearchRequest.builder().page(2).size(25).build().getRequestPage();

        assertThat(page.getPageNumber()).isEqualTo(2);
        assertThat(page.getPageSize()).isEqualTo(25);
        assertThat(page.getSort()).containsExactly(Sort.Order.asc("createdAt"));
    }

    @Test
    void appliesExplicitSortAndDirection() {
        var page = AuditSearchRequest.builder().page(0).size(200)
                .sort("actorIdentity").direction("desc").build().getRequestPage();

        assertThat(page.getSort()).containsExactly(Sort.Order.desc("actorIdentity"));
        assertThat(validator.validate(AuditSearchRequest.builder().page(0).size(200).build())).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void rejectsInvalidSearchParameters(AuditSearchRequest request, String property) {
        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains(property);
    }

    static Stream<Arguments> invalidRequests() {
        var builder = AuditSearchRequest.builder().page(0).size(20);
        return Stream.of(
                Arguments.of(builder.page(-1).build(), "page"),
                Arguments.of(builder.page(0).size(0).build(), "size"),
                Arguments.of(builder.size(201).build(), "size"),
                Arguments.of(builder.size(20).direction("invalid").build(), "direction"),
                Arguments.of(builder.direction("asc").startDate(CREATED_AT).endDate(CREATED_AT).build(),
                        "dateRangeValid"),
                Arguments.of(builder.startDate(CREATED_AT.plusSeconds(1)).build(), "dateRangeValid")
        );
    }

    @Test
    void acceptsOpenEndedAndOrderedDateRanges() {
        var builder = AuditSearchRequest.builder().page(0).size(1);

        assertThat(validator.validate(builder.build())).isEmpty();
        assertThat(validator.validate(builder.startDate(CREATED_AT).build())).isEmpty();
        assertThat(validator.validate(builder.startDate(null).endDate(CREATED_AT).build())).isEmpty();
        assertThat(validator.validate(builder.startDate(CREATED_AT.minusSeconds(1)).build())).isEmpty();
    }

    @Test
    void rejectsUnsupportedSortDirectionsWhenCreatingAPage() {
        var request = AuditSearchRequest.builder().page(0).size(20).direction("sideways").build();

        assertThatThrownBy(request::getRequestPage).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void copyPreservesFieldsAndDetachesMutableCollections() {
        var request = populatedRequest();
        var copy = request.copy().build();

        assertThat(copy).isEqualTo(request).hasSameHashCodeAs(request);
        copy.actorIdentities().add("charlie");
        copy.metadata().get("attempts").add(3);
        copy.metadata().put("new", new ArrayList<>());

        assertThat(request.actorIdentities()).containsExactly("alice", "bob");
        assertThat(request.metadata()).containsOnlyKeys("attempts");
        assertThat(request.metadata().get("attempts")).containsExactly(1, 2);
    }

    @Test
    void copyNormalizesAbsentCollectionsToMutableEmptyCollections() {
        var copy = AuditSearchRequest.builder().build().copy().build();

        assertThat(copy.actorIdentities()).isEmpty();
        assertThat(copy.actorTypes()).isEmpty();
        assertThat(copy.sourceServices()).isEmpty();
        assertThat(copy.correlationIds()).isEmpty();
        assertThat(copy.actions()).isEmpty();
        assertThat(copy.targets()).isEmpty();
        assertThat(copy.statuses()).isEmpty();
        assertThat(copy.ipAddresses()).isEmpty();
        assertThat(copy.metadata()).isEmpty();
        copy.actorIdentities().add("alice");
        copy.metadata().put("attempts", new ArrayList<>());
    }

    @Test
    void equalityAndHashCodeIgnoreFilterOrderAndDuplicates() {
        var original = populatedRequest();
        var reordered = original.copy()
                .actorIdentities(List.of("bob", "alice", "alice"))
                .actorTypes(List.of("SYSTEM", "USER", "USER"))
                .sourceServices(List.of("audit-service", "auth-service"))
                .correlationIds(List.of("second", "first"))
                .actions(List.of(AuditAction.LOGOUT, AuditAction.LOGIN))
                .targets(List.of(AuditTarget.USER, AuditTarget.AUTH))
                .statuses(List.of(AuditStatus.FAILURE, AuditStatus.SUCCESS))
                .ipAddresses(List.of("192.0.2.11", "192.0.2.10"))
                .metadata(Map.of("attempts", List.of(2, 1, 1)))
                .build();

        assertThat(original).isEqualTo(reordered).hasSameHashCodeAs(reordered);
        assertThat(reordered).isEqualTo(original);
        assertThat(original.equals(original)).isTrue();
        assertThat(original.equals(null)).isFalse();
        assertThat(original.equals("request")).isFalse();
    }

    @ParameterizedTest
    @MethodSource("differentRequests")
    void equalityIncludesEveryFilterAndPaginationField(AuditSearchRequest different) {
        assertThat(populatedRequest()).isNotEqualTo(different);
        assertThat(different).isNotEqualTo(populatedRequest());
    }

    static Stream<AuditSearchRequest> differentRequests() {
        var request = populatedRequest();
        return Stream.of(
                request.copy().actorIdentities(List.of("other")).build(),
                request.copy().actorTypes(null).build(),
                request.copy().sourceServices(List.of()).build(),
                request.copy().correlationIds(List.of("other")).build(),
                request.copy().actions(List.of(AuditAction.AUDIT_SEARCH)).build(),
                request.copy().targets(List.of(AuditTarget.AUDIT)).build(),
                request.copy().statuses(List.of(AuditStatus.SUCCESS)).build(),
                request.copy().ipAddresses(List.of("192.0.2.12")).build(),
                request.copy().metadata(null).build(),
                request.copy().metadata(Map.of()).build(),
                request.copy().metadata(Map.of("other", List.of(1, 2))).build(),
                request.copy().metadata(Map.of("attempts", List.of(3))).build(),
                request.copy().startDate(Instant.EPOCH).build(),
                request.copy().endDate(null).build(),
                request.copy().page(1).build(),
                request.copy().size(10).build(),
                request.copy().sort("actorIdentity").build(),
                request.copy().direction("desc").build()
        );
    }

    @Test
    void equalityHandlesNullMetadataValues() {
        Map<String, List<Object>> metadata = new HashMap<>();
        metadata.put("missing", null);
        var first = AuditSearchRequest.builder().metadata(metadata).build();
        var second = AuditSearchRequest.builder().metadata(new HashMap<>(metadata)).build();

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(AuditSearchRequest.builder().build())
                .isEqualTo(AuditSearchRequest.builder().build())
                .hasSameHashCodeAs(AuditSearchRequest.builder().build());
    }

    @Test
    void diagnosticOutputContainsOnlyPopulatedFields() {
        var request = AuditSearchRequest.builder().actorIdentities(List.of("alice"))
                .metadata(Map.of("attempts", List.of(1))).page(0).size(20).build();

        assertThat(request.toString()).contains("actorIdentities=[alice]", "attempts=[1]", "page=0", "size=20")
                .doesNotContain("actorTypes=", "startDate=", "null");
    }

    @Test
    void diagnosticOutputSupportsAbsentMetadata() {
        assertThat(AuditSearchRequest.builder().page(0).size(20).build().toString())
                .contains("Fields=[]", "Pagination=[page=0, size=20]");
    }

    private static AuditSearchRequest populatedRequest() {
        return AuditSearchRequest.builder()
                .actorIdentities(List.of("alice", "bob"))
                .actorTypes(List.of("USER", "SYSTEM"))
                .sourceServices(List.of("auth-service", "audit-service"))
                .correlationIds(List.of("first", "second"))
                .actions(List.of(AuditAction.LOGIN, AuditAction.LOGOUT))
                .targets(List.of(AuditTarget.AUTH, AuditTarget.USER))
                .statuses(List.of(AuditStatus.SUCCESS, AuditStatus.FAILURE))
                .ipAddresses(List.of("192.0.2.10", "192.0.2.11"))
                .metadata(Map.of("attempts", List.of(1, 2)))
                .startDate(CREATED_AT).endDate(CREATED_AT.plusSeconds(60))
                .page(0).size(20).sort("createdAt").direction("asc").build();
    }
}
