package com.bioinformatics.dashboard.audit.event;

import com.bioinformatics.dashboard.audit.dto.AuditAction;
import com.bioinformatics.dashboard.audit.dto.AuditStatus;
import com.bioinformatics.dashboard.audit.service.AuditContextHolder;
import com.bioinformatics.dashboard.audit.service.AuditService;
import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.auth.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationEventListener {
    private final AuditService auditService;
    private final AppUserRepository userRepository;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        var principal = event.getAuthentication().getPrincipal();
        var webDetails = AuditContextHolder.get();
        if (principal instanceof AppUser user) {
            auditService.save(user, AuditAction.LOGIN, null, AuditStatus.SUCCESS, webDetails);
        }
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        var username = event.getAuthentication().getName();
        var user = userRepository.findByUsername(username).orElse(null);
        var webDetails = AuditContextHolder.get();
        auditService.save(user, AuditAction.LOGIN, username, AuditStatus.FAILURE, webDetails);
    }
}
