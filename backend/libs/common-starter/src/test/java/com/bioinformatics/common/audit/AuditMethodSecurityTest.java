package com.bioinformatics.common.audit;

import com.bioinformatics.common.audit.annotation.Auditable;
import com.bioinformatics.common.audit.aspect.AuditAspect;
import com.bioinformatics.common.audit.service.AuditEventPublisher;
import com.bioinformatics.shared.models.audit.AuditAction;
import com.bioinformatics.shared.models.audit.AuditStatus;
import com.bioinformatics.shared.models.audit.AuditTarget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditMethodSecurityTest {

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void auditsMethodSecurityDenialWithoutInvokingTheProtectedMethod() {
        var publisher = mock(AuditEventPublisher.class);
        var authentication = UsernamePasswordAuthenticationToken.authenticated("alice", null,
                AuthorityUtils.createAuthorityList("ROLE_USER"));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        new ApplicationContextRunner().withUserConfiguration(SecurityConfig.class)
                .withBean(AuditAspect.class, () -> new AuditAspect(publisher))
                .withBean(SecuredEndpoint.class, SecuredEndpoint::new)
                .run(context -> {
                    var endpoint = context.getBean(SecuredEndpoint.class);
                    assertThatThrownBy(() -> endpoint.issueToken("alice")).isInstanceOf(AccessDeniedException.class);
                    assertThat(endpoint.invocations()).isZero();
                    verify(publisher).publish(AuditAction.SERVICE_TOKEN_ISSUE, AuditTarget.AUTH,
                            "alice", AuditStatus.FAILURE, authentication);
                });
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy
    @EnableMethodSecurity
    static class SecurityConfig {
    }

    public static class SecuredEndpoint {
        private int invocations;

        @Auditable(action = AuditAction.SERVICE_TOKEN_ISSUE, targetId = "#id")
        @PreAuthorize("hasRole('ADMIN')")
        public String issueToken(String id) {
            invocations++;
            return id;
        }

        public int invocations() {
            return invocations;
        }
    }
}
