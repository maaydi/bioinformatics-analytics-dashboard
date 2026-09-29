package com.bioinformatics.importservice.service;

import com.bioinformatics.shared.models.kafka.events.ViewRefreshRequestedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static com.bioinformatics.shared.models.kafka.KafkaTopics.ANALYTICS_VIEW_REFRESH_REQUESTED;
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
        when(kafkaTemplate.send(eq(ANALYTICS_VIEW_REFRESH_REQUESTED), eq("job-42"), any(ViewRefreshRequestedEvent.class)))
                .thenReturn(future);

        service.refreshAllDashboardViews("job-42", "import");

        verify(kafkaTemplate).send(eq(ANALYTICS_VIEW_REFRESH_REQUESTED), eq("job-42"), any(ViewRefreshRequestedEvent.class));
    }
}

