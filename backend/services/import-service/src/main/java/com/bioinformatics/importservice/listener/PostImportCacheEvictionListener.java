package com.bioinformatics.importservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Job lifecycle listener that clears all caches after successful import completion.
 *
 * <p>Ensures downstream consumers (analytics views, search results, protein lookups)
 * fetch fresh data on next query, preventing stale cached results.
 *
 * <p>Only executes on COMPLETED status; skips on FAILED to preserve current cache state
 * for debugging failed imports.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostImportCacheEvictionListener implements JobExecutionListener {

    private final List<CacheManager> cacheManagers;

    /**
     * Called after import job completes.
     *
     * <p>If job completed successfully, iterates all cache managers and evicts all named caches.
     * Prevents stale results from shadowing newly imported protein data.
     *
     * @param jobExecution batch job execution with final status
     */
    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("[CACHE_LISTENER] Job completed successfully - evicting all caches");

            for (CacheManager cacheManager : cacheManagers) {
                evictCachesForManager(cacheManager);
            }

            log.info("[CACHE_LISTENER] Cache eviction complete - all caches cleared");
        } else {
            log.debug("[CACHE_LISTENER] Job did not complete successfully - status={}, skipping cache eviction",
                    jobExecution.getStatus());
        }
    }

    /**
     * Clears all named caches within a single cache manager.
     *
     * @param manager cache manager to evict
     */
    private void evictCachesForManager(CacheManager manager) {
        log.debug("[CACHE_LISTENER] Evicting caches from manager - cacheCount={}", manager.getCacheNames().size());
        manager.getCacheNames().forEach(cacheName -> {
            var cache = manager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.debug("[CACHE_LISTENER] Cache evicted - name='{}'", cacheName);
            }
        });
    }
}
