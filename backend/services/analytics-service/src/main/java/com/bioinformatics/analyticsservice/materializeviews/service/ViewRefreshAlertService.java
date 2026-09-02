package com.bioinformatics.analyticsservice.materializeviews.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ViewRefreshAlertService {

    public void alertViewRefreshFailure(String jobId, String viewName, int attemptCount, long timeoutMs, String errorMessage) {
        log.error(
                "ALERT materialized-view-refresh-failure jobId={} viewName={} attempts={} timeoutMs={} reason={}",
                jobId,
                viewName,
                attemptCount,
                timeoutMs,
                errorMessage
        );
    }

    public void alertRefreshSequenceFailure(String jobId, List<String> failedViews, long durationMs) {
        log.error(
                "ALERT materialized-view-refresh-sequence-failure jobId={} failedViews={} durationMs={}",
                jobId,
                failedViews,
                durationMs
        );
    }

    public void alertRefreshSequenceSlaBreach(String jobId, long durationMs, long slaMs) {
        log.error(
                "ALERT materialized-view-refresh-sla-breach jobId={} durationMs={} slaMs={}",
                jobId,
                durationMs,
                slaMs
        );
    }
}

