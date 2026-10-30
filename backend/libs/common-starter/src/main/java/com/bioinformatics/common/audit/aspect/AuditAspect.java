package com.bioinformatics.common.audit.aspect;

import com.bioinformatics.common.audit.annotation.Auditable;
import com.bioinformatics.common.audit.service.AuditEventPublisher;
import com.bioinformatics.shared.models.audit.AuditStatus;
import com.bioinformatics.shared.models.audit.AuditTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Aspect
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditEventPublisher publisher;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        if (auditable.skip()) {
            return joinPoint.proceed();
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable error) {
            recordAudit(joinPoint, auditable, null, AuditStatus.FAILURE, authentication);
            throw error;
        }
        var status = result instanceof ResponseEntity<?> response && response.getStatusCode().isError()
                ? AuditStatus.FAILURE : AuditStatus.SUCCESS;
        if (!auditable.auditOnlyOnFailure() || status == AuditStatus.FAILURE) {
            recordAudit(joinPoint, auditable, result, status, authentication);
        }
        return result;
    }

    private void recordAudit(ProceedingJoinPoint joinPoint, Auditable auditable, Object result, AuditStatus status,
                             Authentication authentication) {
        try {
            var target = auditable.target() == AuditTarget.DEFAULT
                    ? auditable.action().getDefaultTarget() : auditable.target();
            publisher.publish(auditable.action(), target,
                    evaluateTargetId(joinPoint, auditable.targetId(), result), status, authentication);
        } catch (RuntimeException error) {
            log.error("Failed to publish audit action={} status={}", auditable.action(), status, error);
        }
    }

    private String evaluateTargetId(ProceedingJoinPoint joinPoint, String expression, Object result) {
        if (expression.isEmpty()) {
            return "N/A";
        }
        try {
            var signature = (MethodSignature) joinPoint.getSignature();
            var context = new MethodBasedEvaluationContext(joinPoint.getTarget(), signature.getMethod(),
                    joinPoint.getArgs(), discoverer);
            context.setVariable("result", result instanceof ResponseEntity<?> response ? response.getBody() : result);
            var value = parser.parseExpression(expression).getValue(context);
            return value != null ? value.toString() : "N/A";
        } catch (RuntimeException error) {
            log.warn("Failed to evaluate audit target expression={}", expression, error);
            return "ERROR_PARSING_ID";
        }
    }
}
