package com.bioinformatics.dashboard.audit.aspect;

import com.bioinformatics.dashboard.audit.annotation.Auditable;
import com.bioinformatics.dashboard.audit.dto.AuditStatus;
import com.bioinformatics.dashboard.audit.service.AuditService;
import com.bioinformatics.dashboard.auth.entity.AppUser;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditService service;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    @AfterReturning(
            pointcut = "@annotation(com.bioinformatics.dashboard.audit.annotation.Auditable)",
            returning = "result"
    )
    public void auditSuccess(JoinPoint joinPoint, Object result) {
        var auditable = getAuditableAnnotation(joinPoint);
        if (auditable == null || auditable.skip()) return;
        if (auditable.auditOnlyOnFailure()) {
            log.debug("Skipping audit for {} (success, auditOnlyOnFailure=true)", auditable.action());
            return;
        }
        var targetId = evaluateSpel(joinPoint, auditable.targetId(), result);
        recordAudit(joinPoint, auditable, targetId, AuditStatus.SUCCESS);
    }

    @AfterThrowing(
            pointcut = "@annotation(com.bioinformatics.dashboard.audit.annotation.Auditable)",
            throwing = "ex"
    )
    public void auditFailure(JoinPoint joinPoint, Exception ex) {
        var auditable = getAuditableAnnotation(joinPoint);
        if (auditable == null || auditable.skip()) return;
        var targetId = evaluateSpel(joinPoint, auditable.targetId(), null);
        recordAudit(joinPoint, auditable, targetId, AuditStatus.FAILURE);
    }


    private void recordAudit(JoinPoint joinPoint,
                             Auditable auditable,
                             String targetId,
                             AuditStatus status
    ) {
        try {
            var requestContext = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestContext != null) {
                var request = requestContext.getRequest();
                var ipAddress = extractIpAddress(request);
                var httpMethod = request.getMethod();
                var endpoint = request.getRequestURI();

                AppUser usr = null;
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getPrincipal() instanceof AppUser user) {
                    usr = user;
                }
                if (usr != null) {
                    service.save(
                            usr,
                            auditable.action(),
                            targetId,
                            status,
                            httpMethod,
                            endpoint, ipAddress
                    );
                }
            }
        } catch (Exception e) {
            log.error("Error during audit logging {}", e.getMessage());
        }
    }

    private Auditable getAuditableAnnotation(JoinPoint joinPoint) {
        try {
            var signature = (MethodSignature) joinPoint.getSignature();
            var target = joinPoint.getTarget();

            var method = target.getClass().getMethod(signature.getName(), signature.getParameterTypes());

            return method.getAnnotation(Auditable.class);
        } catch (Exception e) {
            log.warn("Could not extract @Auditable annotation {}", e.getMessage());
            return null;
        }
    }

    private String extractIpAddress(HttpServletRequest request) {
        String[] ipHeaders = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    private String evaluateSpel(JoinPoint joinPoint, String expressionStr, Object result) {
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
            log.warn("Failed to evaluate audit SpEL expression [{}]: {}", expressionStr, e.getMessage());
            return "ERROR_PARSING_ID";
        }
    }
}

