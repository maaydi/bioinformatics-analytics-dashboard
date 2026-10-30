package com.bioinformatics.authservice.controller;

import com.bioinformatics.authservice.dto.*;
import com.bioinformatics.authservice.service.AuthService;
import com.bioinformatics.common.audit.aspect.AuditAspect;
import com.bioinformatics.common.audit.dto.AuditWebDetails;
import com.bioinformatics.common.audit.service.AuditContextHolder;
import com.bioinformatics.common.audit.service.AuditEventPublisher;
import com.bioinformatics.shared.models.audit.AuditAction;
import com.bioinformatics.shared.models.audit.AuditStatus;
import com.bioinformatics.shared.models.kafka.events.AuditLogEvent;
import com.bioinformatics.shared.models.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.bioinformatics.shared.models.kafka.KafkaTopics.AUDIT_EVENTS_LOG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerAuditTest {
    @Mock
    private AuthService authService;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    private AuthController controller;
    private final UserPrincipal user = new UserPrincipal("alice", List.of("ROLE_USER"), "postgres");

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        AuditContextHolder.set(new AuditWebDetails("POST", "/api/v1/auth", "127.0.0.1", "request-123"));
        var publisher = new AuditEventPublisher(kafkaTemplate, Runnable::run, "auth-service");
        var factory = new AspectJProxyFactory(new AuthController(authService));
        factory.addAspect(new AuditAspect(publisher));
        controller = factory.getProxy();
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        AuditContextHolder.clear();
    }

    @Test
    void auditsLoginWithoutPublishingPasswordOrReturnedTokens() {
        expectSend();
        var request = new LoginRequest("alice", "login-secret");
        var tokens = new TokenResponse("access-secret", "refresh-secret", 3600, "Bearer");
        when(authService.login(request)).thenReturn(tokens);

        assertThat(controller.login(request).getBody()).isSameAs(tokens);

        var event = event();
        assertThat(event.action()).isEqualTo(AuditAction.LOGIN);
        assertThat(event.targetId()).isEqualTo("alice");
        assertThat(event.status()).isEqualTo(AuditStatus.SUCCESS);
        assertThat(event.actorType()).isEqualTo("ANONYMOUS");
        assertThat(event.toString()).doesNotContain("login-secret", "access-secret", "refresh-secret");
    }

    @Test
    void auditsFailedLoginWithAttemptedUsernameButNoPassword() {
        expectSend();
        var request = new LoginRequest("alice", "wrong-secret");
        var original = new BadCredentialsException("Invalid credentials");
        when(authService.login(request)).thenThrow(original);

        assertThatThrownBy(() -> controller.login(request)).isSameAs(original);

        var event = event();
        assertThat(event.targetId()).isEqualTo("alice");
        assertThat(event.status()).isEqualTo(AuditStatus.FAILURE);
        assertThat(event.toString()).doesNotContain("wrong-secret");
    }

    @Test
    void successfulRefreshIsNotAudited() {
        var request = new RefreshRequest("refresh-secret");
        when(authService.refresh(request)).thenReturn(new TokenResponse("new-access", "new-refresh", 3600, "Bearer"));

        controller.refresh(request);

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void failedRefreshIsAuditedWithoutTheToken() {
        expectSend();
        var request = new RefreshRequest("invalid-refresh-secret");
        var original = new BadCredentialsException("Invalid token");
        when(authService.refresh(request)).thenThrow(original);

        assertThatThrownBy(() -> controller.refresh(request)).isSameAs(original);

        var event = event();
        assertThat(event.action()).isEqualTo(AuditAction.TOKEN_REFRESH);
        assertThat(event.status()).isEqualTo(AuditStatus.FAILURE);
        assertThat(event.toString()).doesNotContain("invalid-refresh-secret");
    }

    @Test
    void logoutRetainsActorAfterServiceClearsSecurityContext() {
        expectSend();
        authenticate();
        doAnswer(ignored -> {
            SecurityContextHolder.clearContext();
            return null;
        }).when(authService).logout("alice");

        controller.logout(user);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        var event = event();
        assertThat(event.action()).isEqualTo(AuditAction.LOGOUT);
        assertThat(event.actorIdentity()).isEqualTo("alice");
        assertThat(event.actorType()).isEqualTo("USER");
        assertThat(event.targetId()).isEqualTo("alice");
    }

    @Test
    void passwordChangeRetainsActorWithoutPublishingPasswords() {
        expectSend();
        authenticate();
        var request = new ChangePasswordRequest("old-secret", "new-secret");
        when(authService.updatePassword(request, "alice")).thenAnswer(ignored -> {
            SecurityContextHolder.clearContext();
            return ChangePasswordResponse.succeed();
        });

        controller.updatePassword(request, user);

        var event = event();
        assertThat(event.action()).isEqualTo(AuditAction.PASSWORD_CHANGE);
        assertThat(event.actorIdentity()).isEqualTo("alice");
        assertThat(event.targetId()).isEqualTo("alice");
        assertThat(event.toString()).doesNotContain("old-secret", "new-secret");
    }

    @Test
    void serviceTokenIssuanceNeverPublishesTheCredential() {
        expectSend();
        authenticate();
        when(authService.issueServiceToken("alice")).thenReturn(TokenResponse.serviceBearer("service-secret", 300));

        controller.issueServiceToken(user);

        var event = event();
        assertThat(event.action()).isEqualTo(AuditAction.SERVICE_TOKEN_ISSUE);
        assertThat(event.targetId()).isEqualTo("alice");
        assertThat(event.toString()).doesNotContain("service-secret");
    }

    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
    }

    private void expectSend() {
        when(kafkaTemplate.send(eq(AUDIT_EVENTS_LOG), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    private AuditLogEvent event() {
        var captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        verify(kafkaTemplate).send(eq(AUDIT_EVENTS_LOG), anyString(), captor.capture());
        assertThat(captor.getValue().metadata()).containsExactly(
                "httpMethod", "POST", "endpoint", "/api/v1/auth");
        return captor.getValue();
    }
}
