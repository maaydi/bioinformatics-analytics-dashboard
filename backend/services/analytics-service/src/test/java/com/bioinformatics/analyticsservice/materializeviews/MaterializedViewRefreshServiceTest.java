package com.bioinformatics.analyticsservice.materializeviews;

import com.bioinformatics.analyticsservice.config.ApplicationProperties;
import com.bioinformatics.analyticsservice.materializeviews.dto.RefreshResult;
import com.bioinformatics.analyticsservice.materializeviews.dto.ViewToRefresh;
import com.bioinformatics.analyticsservice.materializeviews.entity.ViewRefreshLog;
import com.bioinformatics.analyticsservice.materializeviews.repository.ViewRefreshLogRepository;
import com.bioinformatics.analyticsservice.materializeviews.service.MaterializedViewRefreshService;
import com.bioinformatics.analyticsservice.materializeviews.service.ViewRefreshAlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterializedViewRefreshServiceTest {

    @Spy
    private final ApplicationProperties appProperties = new ApplicationProperties(
            new ApplicationProperties.ViewRefresh(3, 1000, 0, 10_000)
    );
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private ViewRefreshLogRepository logRepository;
    @Mock
    private ViewRefreshAlertService alertService;
    @InjectMocks
    private MaterializedViewRefreshService service;

    @Test
    void executeAndLogRefresh_successOnFirstAttempt_savesSuccessLog() {
        var result = service.executeAndLogRefresh("job-1", new ViewToRefresh("mv_dashboard_kpis", false));

        assertThat(result.success()).isTrue();
        verify(jdbcTemplate, times(1)).execute(anyConnectionCallback());
        var logCaptor = ArgumentCaptor.forClass(ViewRefreshLog.class);
        verify(logRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().isSuccess()).isTrue();
        verifyNoInteractions(alertService);
    }

    @Test
    void executeAndLogRefresh_retriesAndSucceedsBeforeExhaustion() {
        doThrow(new RuntimeException("transient"))
                .doAnswer(ignored -> null)
                .when(jdbcTemplate).execute(anyConnectionCallback());

        var result = service.executeAndLogRefresh("job-2", new ViewToRefresh("mv_keyword_frequency", true));

        assertThat(result.success()).isTrue();
        verify(jdbcTemplate, times(2)).execute(anyConnectionCallback());
        verify(logRepository, times(1)).save(any());
        verifyNoInteractions(alertService);
    }

    @Test
    void executeAndLogRefresh_exhaustedRetries_alertsAndSavesFailureLog() {
        doThrow(new RuntimeException("timeout"))
                .when(jdbcTemplate).execute(anyConnectionCallback());

        var result = service.executeAndLogRefresh("job-3", new ViewToRefresh("mv_organism_counts", true));

        assertThat(result.success()).isFalse();
        verify(jdbcTemplate, times(3)).execute(anyConnectionCallback());

        var logCaptor = ArgumentCaptor.forClass(ViewRefreshLog.class);
        verify(logRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().isSuccess()).isFalse();
        assertThat(logCaptor.getValue().getErrorMessage()).contains("timeout");

        verify(alertService).alertViewRefreshFailure(eq("job-3"), eq("mv_organism_counts"), eq(3), eq(1000L), contains("timeout"));
    }

    @Test
    @SuppressWarnings({"SqlNoDataSourceInspection", "SqlResolve", "resource"})
    void executeAndLogRefresh_executesTimeoutAndResetStatements() throws Exception {
        var connection = mock(Connection.class);
        var refreshStatement = mock(Statement.class);
        var resetStatement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(refreshStatement, resetStatement);

        doAnswer(invocation -> {
            var callback = invocation.getArgument(0, ConnectionCallback.class);
            callback.doInConnection(connection);
            return null;
        }).when(jdbcTemplate).execute(anyConnectionCallback());

        var result = service.executeAndLogRefresh("job-4", new ViewToRefresh("mv_keyword_frequency", true));

        assertThat(result.success()).isTrue();
        verify(refreshStatement).execute("SET statement_timeout = 1000");
        verify(refreshStatement).execute("REFRESH MATERIALIZED VIEW CONCURRENTLY \"mv_keyword_frequency\"");
        verify(resetStatement).execute("SET statement_timeout = DEFAULT");
    }

    @Test
    void refreshAllDashboardViews_whenFailuresAndSlaBreach_emitsSequenceAlerts() {
        when(appProperties.viewRefresh().sequenceSlaMs()).thenReturn(1L);

        var spiedService = spy(service);
        var results = List.of(
                new RefreshResult("mv_dashboard_kpis", true),
                new RefreshResult("mv_length_histogram", false),
                new RefreshResult("mv_organism_counts", true),
                new RefreshResult("mv_reviewed_ratio", true),
                new RefreshResult("mv_evidence_distribution", false),
                new RefreshResult("mv_keyword_frequency", true)
        );
        var index = new AtomicInteger(0);

        doAnswer(invocation -> {
            Thread.sleep(2);
            return results.get(index.getAndIncrement());
        }).when(spiedService).executeAndLogRefresh(eq("job-seq"), any(ViewToRefresh.class));

        spiedService.refreshAllDashboardViews("job-seq");

        verify(alertService).alertRefreshSequenceFailure(eq("job-seq"), eq(List.of("mv_length_histogram", "mv_evidence_distribution")), anyLong());
        verify(alertService).alertRefreshSequenceSlaBreach(eq("job-seq"), anyLong(), eq(1L));
    }

    @SuppressWarnings("unchecked")
    private ConnectionCallback<Object> anyConnectionCallback() {
        return (ConnectionCallback<Object>) any(ConnectionCallback.class);
    }
}

