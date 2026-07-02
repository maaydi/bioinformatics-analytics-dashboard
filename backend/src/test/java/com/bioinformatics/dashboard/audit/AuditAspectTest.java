package com.bioinformatics.dashboard.audit;

import com.bioinformatics.dashboard.audit.annotation.Auditable;
import com.bioinformatics.dashboard.audit.aspect.AuditAspect;
import com.bioinformatics.dashboard.audit.service.AuditService;
import com.bioinformatics.dashboard.model.audit.AuditAction;
import com.bioinformatics.dashboard.model.audit.AuditStatus;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private AuditService auditService;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private AuditAspect auditAspect;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void auditSuccess_shouldLogSuccess() throws Exception {
        DummyController target = new DummyController();
        Method method = DummyController.class.getMethod("getDetail", String.class);

        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"123"});
        when(methodSignature.getName()).thenReturn(method.getName());
        when(methodSignature.getParameterTypes()).thenReturn(method.getParameterTypes());
        when(methodSignature.getMethod()).thenReturn(method);

        auditAspect.auditSuccess(joinPoint, ResponseEntity.ok("test"));

        verify(auditService).save(any(), any(), eq(AuditAction.DETAIL_VIEW), eq("123"), eq(AuditStatus.SUCCESS), any());
    }

    @Test
    void auditFailure_shouldLogFailure() throws Exception {
        DummyController target = new DummyController();
        Method method = DummyController.class.getMethod("getDetail", String.class);

        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"999"});
        when(methodSignature.getName()).thenReturn(method.getName());
        when(methodSignature.getParameterTypes()).thenReturn(method.getParameterTypes());
        when(methodSignature.getMethod()).thenReturn(method);

        auditAspect.auditFailure(joinPoint, new RuntimeException("Error"));

        verify(auditService).save(any(), any(), eq(AuditAction.DETAIL_VIEW), eq("999"), eq(AuditStatus.FAILURE), any());
    }

    @Test
    void auditSuccess_shouldSkipIfAuditOnlyOnFailureIsTrue() throws Exception {
        DummyController target = new DummyController();
        Method method = DummyController.class.getMethod("executeSearch");

        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn(method.getName());
        when(methodSignature.getParameterTypes()).thenReturn(method.getParameterTypes());

        auditAspect.auditSuccess(joinPoint, null);

        verifyNoInteractions(auditService);
    }

    @Test
    void auditSuccess_shouldSkipIfSkipIsTrue() throws Exception {
        DummyController target = new DummyController();
        Method method = DummyController.class.getMethod("skipped");

        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getName()).thenReturn(method.getName());
        when(methodSignature.getParameterTypes()).thenReturn(method.getParameterTypes());

        auditAspect.auditSuccess(joinPoint, null);

        verifyNoInteractions(auditService);
    }

    static class DummyController {
        @Auditable(action = AuditAction.DETAIL_VIEW, targetId = "#id")
        public ResponseEntity<String> getDetail(String id) {
            return ResponseEntity.ok("test");
        }

        @Auditable(action = AuditAction.SEARCH_QUERY, auditOnlyOnFailure = true)
        public void executeSearch() {
        }

        @Auditable(action = AuditAction.DETAIL_VIEW, skip = true)
        public void skipped() {
        }
    }
}

