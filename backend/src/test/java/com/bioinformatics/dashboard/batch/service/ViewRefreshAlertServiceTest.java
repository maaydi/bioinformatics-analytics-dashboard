package com.bioinformatics.dashboard.batch.service;

import com.bioinformatics.dashboard.job.service.ViewRefreshAlertService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;

class ViewRefreshAlertServiceTest {

    private final ViewRefreshAlertService service = new ViewRefreshAlertService();

    @Test
    void alertViewRefreshFailure_logsWithoutThrowing() {
        assertThatCode(() -> service.alertViewRefreshFailure("job-1", "mv_dashboard_kpis", 3, 1000L, "timeout"))
                .doesNotThrowAnyException();
    }

    @Test
    void alertRefreshSequenceFailure_logsWithoutThrowing() {
        var failedViews = List.of("mv_length_histogram", "mv_keyword_frequency");

        assertThatCode(() -> service.alertRefreshSequenceFailure("job-2", failedViews, 2200L))
                .doesNotThrowAnyException();
    }

    @Test
    void alertRefreshSequenceSlaBreach_logsWithoutThrowing() {
        assertThatCode(() -> service.alertRefreshSequenceSlaBreach("job-3", 5400L, 5000L))
                .doesNotThrowAnyException();
    }
}

