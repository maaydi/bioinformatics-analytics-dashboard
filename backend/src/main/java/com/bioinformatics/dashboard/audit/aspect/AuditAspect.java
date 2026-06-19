package com.bioinformatics.dashboard.audit.aspect;

import com.bioinformatics.dashboard.audit.annotation.Auditable;
import com.bioinformatics.dashboard.audit.dto.AuditAction;
import com.bioinformatics.dashboard.audit.dto.AuditStatus;
import com.bioinformatics.dashboard.audit.service.AuditService;
import com.bioinformatics.dashboard.auth.dto.LoginRequest;
import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.auth.repository.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;


@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditService service;
    private final AppUserRepository userRepository;

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
        recordAudit(joinPoint, auditable, AuditStatus.SUCCESS);
    }

    @AfterThrowing(
            pointcut = "@annotation(com.bioinformatics.dashboard.audit.annotation.Auditable)",
            throwing = "ex"
    )
    public void auditFailure(JoinPoint joinPoint, Exception ex) {
        var auditable = getAuditableAnnotation(joinPoint);
        if (auditable == null || auditable.skip()) return;

        recordAudit(joinPoint, auditable, AuditStatus.FAILURE);
    }


    private void recordAudit(JoinPoint joinPoint,
                             Auditable auditable,
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
                // LOGIN user is saved in security context after audit is called
                if (auditable.action() == AuditAction.LOGIN) {
                    usr = getLoggedInUser(joinPoint).orElse(null);
                }
                // usr is null ==> attempt to log in with unregistered username so no audit / Security filter will reject request
                if (usr != null) {
                    service.save(
                            usr,
                            auditable.action(),
                            "TargetId",
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

    private Optional<AppUser> getLoggedInUser(JoinPoint joinPoint) {
        for (var arg : joinPoint.getArgs()) {
            if (arg instanceof LoginRequest loginRequest) {
                return userRepository.findByUsername(loginRequest.username());
            }
        }
        return Optional.empty();
    }
}

