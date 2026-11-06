package com.bioinformatics.importservice.service;

import com.bioinformatics.common.correlation.CorrelationContext;
import com.bioinformatics.shared.models.kafka.events.ViewRefreshRequestedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static com.bioinformatics.shared.models.kafka.KafkaTopics.PROTEIN_EVENTS_IMPORTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterializedViewRefreshServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private MaterializedViewRefreshService service;

    @Test
    void refreshAllDashboardViewsPublishesKafkaEvent() {
        var future = new CompletableFuture<SendResult<String, Object>>();
        when(kafkaTemplate.send(eq(PROTEIN_EVENTS_IMPORTED), eq("job-42"), any(ViewRefreshRequestedEvent.class)))
                .thenAnswer(invocation -> {
                    var event = invocation.getArgument(2, ViewRefreshRequestedEvent.class);
                    assertThat(event.correlationId()).isEqualTo(MDC.get(CorrelationContext.MDC_KEY));
                    return future;
                });

        service.refreshAllDashboardViews("job-42", "import");

        verify(kafkaTemplate).send(eq(PROTEIN_EVENTS_IMPORTED), eq("job-42"), any(ViewRefreshRequestedEvent.class));
        assertThat(MDC.get(CorrelationContext.MDC_KEY)).isNull();
    }
}
