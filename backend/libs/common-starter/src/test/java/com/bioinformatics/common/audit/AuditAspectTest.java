package com.bioinformatics.common.audit;

import com.bioinformatics.common.audit.annotation.Auditable;
import com.bioinformatics.common.audit.aspect.AuditAspect;
import com.bioinformatics.common.audit.service.AuditEventPublisher;
import com.bioinformatics.shared.models.audit.AuditAction;
import com.bioinformatics.shared.models.audit.AuditStatus;
import com.bioinformatics.shared.models.audit.AuditTarget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private AuditEventPublisher publisher;
    private AuditedService service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        var factory = new AspectJProxyFactory(new AuditedService());
        factory.addAspect(new AuditAspect(publisher));
        service = factory.getProxy();
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publishesSuccessWithArgumentAndActionDefaultTarget() {
        assertThat(service.detail("123").getBody()).isEqualTo("123");
        verify(publisher).publish(AuditAction.DETAIL_VIEW, AuditTarget.DETAIL, "123", AuditStatus.SUCCESS, null);
    }

    @Test
    void publishesFailureWithoutReplacingOriginalException() {
        var original = new IllegalStateException("business failure");
        assertThatThrownBy(() -> service.fail("456", original)).isSameAs(original);
        verify(publisher).publish(AuditAction.FILTER_DELETE, AuditTarget.SAVED_FILTER, "456", AuditStatus.FAILURE, null);
    }

    @Test
    void failureOnlyAuditsOnlyFailure() {
        service.failureOnly(false);
        verifyNoInteractions(publisher);
        assertThatThrownBy(() -> service.failureOnly(true)).isInstanceOf(IllegalStateException.class);
        verify(publisher).publish(AuditAction.SEARCH_QUERY, AuditTarget.SEARCH, "N/A", AuditStatus.FAILURE, null);
    }

    @Test
    void skipSuppressesBothSuccessAndFailure() {
        service.skipped(false);
        assertThatThrownBy(() -> service.skipped(true)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(publisher);
    }

    @Test
    void unwrapsResponseEntityAndHonorsExplicitTarget() {
        service.create();
        verify(publisher).publish(AuditAction.FILTER_SAVE, AuditTarget.USER, "42", AuditStatus.SUCCESS, null);
    }

    @Test
    void handlesVoidResultsWithoutAResultId() {
        service.plainResult();
        verify(publisher).publish(AuditAction.FILTER_SAVE, AuditTarget.SAVED_FILTER, "N/A", AuditStatus.SUCCESS, null);
    }

    @Test
    void evaluatesPlainRecords() {
        assertThat(service.plainRecord().id()).isEqualTo("42");
        verify(publisher).publish(AuditAction.FILTER_SAVE, AuditTarget.SAVED_FILTER, "42", AuditStatus.SUCCESS, null);
    }

    @Test
    void invalidExpressionDoesNotReplaceResult() {
        assertThat(service.invalidExpression()).isEqualTo("result");
        verify(publisher).publish(AuditAction.DETAIL_VIEW, AuditTarget.DETAIL,
                "ERROR_PARSING_ID", AuditStatus.SUCCESS, null);
    }

    @Test
    void publisherFailureDoesNotReplaceBusinessResultOrException() {
        doThrow(new IllegalStateException("publisher failure")).when(publisher)
                .publish(AuditAction.DETAIL_VIEW, AuditTarget.DETAIL, "123", AuditStatus.SUCCESS, null);
        assertThat(service.detail("123").getBody()).isEqualTo("123");
        doThrow(new IllegalStateException("publisher failure")).when(publisher)
                .publish(AuditAction.FILTER_DELETE, AuditTarget.SAVED_FILTER, "456", AuditStatus.FAILURE, null);
        var original = new IllegalArgumentException("business failure");
        assertThatThrownBy(() -> service.fail("456", original)).isSameAs(original);
    }

    @Test
    void preservesEntryAuthenticationWhenTheBusinessMethodClearsSecurityContext() {
        var authentication = UsernamePasswordAuthenticationToken.authenticated("alice", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        service.clearAuthentication();

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(publisher).publish(AuditAction.LOGOUT, AuditTarget.AUTH, "N/A", AuditStatus.SUCCESS, authentication);
    }

    @Test
    void errorResponseIsFailureEvenWithoutAnException() {
        assertThat(service.deniedResponse().getStatusCode().value()).isEqualTo(403);
        verify(publisher).publish(AuditAction.DETAIL_VIEW, AuditTarget.DETAIL, "N/A", AuditStatus.FAILURE, null);
    }

    public static class AuditedService {
        @Auditable(action = AuditAction.DETAIL_VIEW, targetId = "#id")
        public ResponseEntity<String> detail(String id) {
            return ResponseEntity.ok(id);
        }

        @Auditable(action = AuditAction.FILTER_DELETE, targetId = "#id")
        public void fail(String id, RuntimeException error) {
            throw error;
        }

        @Auditable(action = AuditAction.SEARCH_QUERY, auditOnlyOnFailure = true)
        public void failureOnly(boolean fail) {
            if (fail) {
                throw new IllegalStateException("failure");
            }
        }

        @Auditable(action = AuditAction.DETAIL_VIEW, skip = true)
        public void skipped(boolean fail) {
            if (fail) {
                throw new IllegalStateException("failure");
            }
        }

        @Auditable(action = AuditAction.FILTER_SAVE, target = AuditTarget.USER, targetId = "#result.id")
        public ResponseEntity<Result> create() {
            return ResponseEntity.ok(new Result("42"));
        }

        @Auditable(action = AuditAction.FILTER_SAVE, targetId = "#result?.id")
        public void plainResult() {
            new Result("42");
        }

        @Auditable(action = AuditAction.FILTER_SAVE, targetId = "#result.id")
        public Result plainRecord() {
            return new Result("42");
        }

        @Auditable(action = AuditAction.LOGOUT)
        public void clearAuthentication() {
            SecurityContextHolder.clearContext();
        }

        @Auditable(action = AuditAction.DETAIL_VIEW, auditOnlyOnFailure = true)
        public ResponseEntity<Void> deniedResponse() {
            return ResponseEntity.status(403).build();
        }

        @Auditable(action = AuditAction.DETAIL_VIEW, targetId = "#missing.id")
        public String invalidExpression() {
            return "result";
        }
    }

    public record Result(String id) {
    }
}
