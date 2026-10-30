package com.bioinformatics.common.audit;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bioinformatics.common.audit.dto.AuditWebDetails;
import com.bioinformatics.common.audit.service.AuditContextHolder;
import com.bioinformatics.common.audit.service.AuditEventPublisher;
import com.bioinformatics.shared.models.audit.AuditAction;
import com.bioinformatics.shared.models.audit.AuditStatus;
import com.bioinformatics.shared.models.audit.AuditTarget;
import com.bioinformatics.shared.models.kafka.events.AuditLogEvent;
import com.bioinformatics.shared.models.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.bioinformatics.shared.models.kafka.KafkaTopics.AUDIT_EVENTS_LOG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private TaskExecutor executor;
    private AuditEventPublisher publisher;
    private final Logger logger = (Logger) LoggerFactory.getLogger(AuditEventPublisher.class);
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        publisher = new AuditEventPublisher(kafkaTemplate, executor, "test-service");
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        AuditContextHolder.clear();
        MDC.clear();
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void snapshotsCallerContextBeforeAsynchronousSend() {
        var principal = new UserPrincipal("user-123", List.of("ROLE_USER"), "postgres");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
        AuditContextHolder.set(new AuditWebDetails("POST", "/filters", "127.0.0.1", "request-123"));
        var before = Instant.now();

        publisher.publish(AuditAction.FILTER_SAVE, AuditTarget.SAVED_FILTER, "42", AuditStatus.SUCCESS);

        verifyNoInteractions(kafkaTemplate);
        SecurityContextHolder.clearContext();
        AuditContextHolder.clear();
        var event = runQueuedSend();

        assertThat(event.eventId()).isNotNull();
        assertThat(event.actorIdentity()).isEqualTo("user-123");
        assertThat(event.actorType()).isEqualTo("USER");
        assertThat(event.sourceService()).isEqualTo("test-service");
        assertThat(event.correlationId()).isEqualTo("request-123");
        assertThat(event.action()).isEqualTo(AuditAction.FILTER_SAVE);
        assertThat(event.targetType()).isEqualTo(AuditTarget.SAVED_FILTER);
        assertThat(event.targetId()).isEqualTo("42");
        assertThat(event.status()).isEqualTo(AuditStatus.SUCCESS);
        assertThat(event.ipAddress()).isEqualTo("127.0.0.1");
        assertThat(event.createdAt()).isBetween(before, Instant.now());
        assertThat(event.metadata()).containsExactly("httpMethod", "POST", "endpoint", "/filters");
    }

    @Test
    void usesAuthenticationNameForNonUserPrincipalAndTracingCorrelation() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("jwt-user", null, List.of()));
        MDC.put("traceId", "trace-123");

        publisher.publish(AuditAction.LOGIN, AuditTarget.DEFAULT, null, AuditStatus.SUCCESS);
        var event = runQueuedSend();

        assertThat(event.actorIdentity()).isEqualTo("jwt-user");
        assertThat(event.correlationId()).isEqualTo("trace-123");
        assertThat(event.targetType()).isEqualTo(AuditTarget.AUTH);
    }

    @Test
    void publishesSystemEventsWithoutServletOrSecurityContext() {
        publisher.publish(AuditAction.IMPORT_UPLOAD, AuditTarget.IMPORT_JOB, "job", AuditStatus.SUCCESS);
        var event = runQueuedSend();

        assertThat(event.actorIdentity()).isEqualTo("UNKNOWN");
        assertThat(event.actorType()).isEqualTo("SYSTEM");
        assertThat(event.correlationId()).isNotBlank();
        assertThat(event.ipAddress()).isNull();
        assertThat(event.metadata()).containsExactly("httpMethod", "SYSTEM", "endpoint", "INTERNAL");
    }

    @Test
    void distinguishesAnonymousWebRequests() {
        AuditContextHolder.set(new AuditWebDetails("POST", "/login", "127.0.0.1", null));
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        publisher.publish(AuditAction.LOGIN, AuditTarget.AUTH, null, AuditStatus.FAILURE);

        assertThat(runQueuedSend().actorType()).isEqualTo("ANONYMOUS");
    }

    @Test
    void logsQueueRejectionWithoutBlockingOrThrowing() {
        doThrow(new TaskRejectedException("full")).when(executor).execute(any(Runnable.class));

        assertThatCode(() -> publisher.publish(AuditAction.LOGIN, AuditTarget.AUTH, null, AuditStatus.SUCCESS))
                .doesNotThrowAnyException();

        verifyNoInteractions(kafkaTemplate);
        assertThat(appender.list).anySatisfy(entry ->
                assertThat(entry.getFormattedMessage()).contains("queue rejected", "eventId="));
    }

    @Test
    void logsImmediateKafkaFailure() {
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(executor).execute(any(Runnable.class));
        when(kafkaTemplate.send(eq(AUDIT_EVENTS_LOG), anyString(), any()))
                .thenThrow(new IllegalStateException("broker unavailable"));

        assertThatCode(() -> publisher.publish(AuditAction.LOGIN, AuditTarget.AUTH, null, AuditStatus.SUCCESS))
                .doesNotThrowAnyException();
        assertThat(appender.list).anySatisfy(entry ->
                assertThat(entry.getFormattedMessage()).contains("Audit send failed", "eventId="));
    }

    @Test
    void logsAsynchronousKafkaFailure() {
        var future = new CompletableFuture<SendResult<String, Object>>();
        when(kafkaTemplate.send(eq(AUDIT_EVENTS_LOG), anyString(), any())).thenReturn(future);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(executor).execute(any(Runnable.class));
        publisher.publish(AuditAction.LOGIN, AuditTarget.AUTH, null, AuditStatus.SUCCESS);

        future.completeExceptionally(new IllegalStateException("broker unavailable"));

        assertThat(appender.list).anySatisfy(entry ->
                assertThat(entry.getFormattedMessage()).contains("Audit publish failed", "eventId="));
    }

    private AuditLogEvent runQueuedSend() {
        var task = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(task.capture());
        when(kafkaTemplate.send(eq(AUDIT_EVENTS_LOG), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        task.getValue().run();
        var event = ArgumentCaptor.forClass(AuditLogEvent.class);
        var key = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(AUDIT_EVENTS_LOG), key.capture(), event.capture());
        assertThat(key.getValue()).isEqualTo(event.getValue().eventId().toString());
        return event.getValue();
    }
}
