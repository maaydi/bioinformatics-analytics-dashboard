package com.bioinformatics.dashboard.job.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Clears all caches after successful protein import job completion.
 * Ensures downstream consumers (analytics views, search results) refresh with new data.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostImportCacheEvictionListener implements JobExecutionListener {

    private final List<CacheManager> cacheManagers;

    /**
     * On successful job completion, evicts all caches across all managers.
     * Prevents stale cached results from shadowing newly imported protein data.
     */
    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("Batch job completed successfully. Evicting all Redis caches...");

            for (CacheManager cacheManager : cacheManagers) {
                evictCachesForManager(cacheManager);
            }

            log.info("Cache eviction complete.");
        } else {
            log.warn("Batch job did not complete successfully (Status: {}). Skipping cache eviction.", jobExecution.getStatus());
        }
    }

    /**
     * Clears all named caches within a single cache manager.
     */
    private void evictCachesForManager(CacheManager manager) {
        manager.getCacheNames().forEach(cacheName -> {
            var cache = manager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.debug("Cleared cache: {}", cacheName);
            }
        });
    }
}
