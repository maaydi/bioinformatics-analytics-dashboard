package com.bioinformatics.importservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MaterializedViewRefreshService {

    public void refreshAllDashboardViews(String jobId) {
        log.info("Starting materialized views refresh sequence for job: {}", jobId);
        // TODO send message to kafka
    }
}