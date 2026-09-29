package com.bioinformatics.dashboard.audit.aspect;

import com.bioinformatics.dashboard.audit.annotation.Auditable;
import com.bioinformatics.dashboard.audit.dto.AuditStatus;
import com.bioinformatics.dashboard.audit.service.AuditContextHolder;
import com.bioinformatics.dashboard.audit.service.AuditService;
import com.bioinformatics.shared.models.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
/**
 * AOP aspect that records authenticated action audits for methods annotated with {@link Auditable}.
 *
 * <p>The aspect captures both successful and failing execution paths and stores the outcome in the central audit log,
 * including a target identifier resolved from a SpEL expression when one is configured. This lets the dashboard
 * maintain a read-only trail of sensitive actions such as searches, filter saves, and data exports.</p>
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditService service;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    /**
     * Handles successful method executions annotated with {@code @Auditable} and records a success audit.
     *
     * @param joinPoint the join point of the executed method
     * @param result the returned value from the method, or {@code null}
     */
    @AfterReturning(
            pointcut = "@annotation(com.bioinformatics.dashboard.audit.annotation.Auditable)",
            returning = "result"
    )
    public void auditSuccess(JoinPoint joinPoint, Object result) {
        var auditable = getAuditableAnnotation(joinPoint);
        if (auditable == null || auditable.skip()) return;
        if (auditable.auditOnlyOnFailure()) {
            log.debug("[DASHBOARD][AUDIT] Skipping success audit for action={} because auditOnlyOnFailure=true", auditable.action());
            return;
        }
        var targetId = evaluateSPEL(joinPoint, auditable.targetId(), result);
        recordAudit(auditable, targetId, AuditStatus.SUCCESS);
    }

    /**
     * Handles exceptions thrown by methods annotated with {@code @Auditable} and records a failure audit.
     *
     * @param joinPoint the join point of the executed method
     * @param ex the exception thrown by the method
     */
    @AfterThrowing(
            pointcut = "@annotation(com.bioinformatics.dashboard.audit.annotation.Auditable)",
            throwing = "ex"
    )
    public void auditFailure(JoinPoint joinPoint, Exception ex) {
        var auditable = getAuditableAnnotation(joinPoint);
        if (auditable == null || auditable.skip()) return;
        var targetId = evaluateSPEL(joinPoint, auditable.targetId(), null);
        recordAudit(auditable, targetId, AuditStatus.FAILURE);
    }

    private void recordAudit(Auditable auditable,
                             String targetId,
                             AuditStatus status
    ) {
        try {
            UserPrincipal usr = null;
            String userName = null;
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal user) {
                usr = user;
                userName = authentication.getName();
            }
            var webDetails = AuditContextHolder.get();
            service.save(usr, userName, auditable.action(), targetId, status, webDetails);
        } catch (Exception e) {
            log.error("[DASHBOARD][AUDIT] Error while persisting audit log action={} status={} targetId={} error={}", auditable.action(), status, targetId, e.getMessage(), e);
        }
    }

    private Auditable getAuditableAnnotation(JoinPoint joinPoint) {
        try {
            var signature = (MethodSignature) joinPoint.getSignature();
            var target = joinPoint.getTarget();

            var method = target.getClass().getMethod(signature.getName(), signature.getParameterTypes());

            return method.getAnnotation(Auditable.class);
        } catch (Exception e) {
            log.warn("[DASHBOARD][AUDIT] Could not extract @Auditable annotation for method={} error={}", joinPoint.getSignature().getName(), e.getMessage());
            return null;
        }
    }

    private String evaluateSPEL(JoinPoint joinPoint, String expressionStr, Object result) {
        if (expressionStr.isEmpty()) return "N/A";

        try {
            var signature = (MethodSignature) joinPoint.getSignature();
            var context = new MethodBasedEvaluationContext(
                    joinPoint.getTarget(),
                    signature.getMethod(),
                    joinPoint.getArgs(),
                    discoverer
            );

            if (result != null) {
                if (result instanceof ResponseEntity<?> responseEntity) {
                    context.setVariable("result", responseEntity.getBody());
                } else {
                    context.setVariable("result", result);
                }
            }

            var expression = parser.parseExpression(expressionStr);
            var value = expression.getValue(context);
            return value != null ? value.toString() : "N/A";
        } catch (Exception e) {
            log.warn("[DASHBOARD][AUDIT] Failed to evaluate audit SpEL expression={} error={}", expressionStr, e.getMessage());
            return "ERROR_PARSING_ID";
        }
    }
}