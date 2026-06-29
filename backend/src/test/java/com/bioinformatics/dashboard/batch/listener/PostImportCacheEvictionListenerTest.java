package com.bioinformatics.dashboard.batch.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostImportCacheEvictionListenerTest {

    @Mock
    private CacheManager cacheManager1;

    @Mock
    private CacheManager cacheManager2;

    @Mock
    private Cache cacheA;

    @Mock
    private Cache cacheB;

    private PostImportCacheEvictionListener listener;

    private JobExecution jobExecution;

    @BeforeEach
    void setUp() {
        listener = new PostImportCacheEvictionListener(List.of(cacheManager1, cacheManager2));
        jobExecution = org.springframework.batch.test.MetaDataInstanceFactory.createJobExecution("job", 1L, 1L, new org.springframework.batch.core.job.parameters.JobParameters());
    }

    @Test
    void afterJob_whenCompleted_clearsAllCaches() {
        when(cacheManager1.getCacheNames()).thenReturn(Set.of("cacheA"));
        when(cacheManager1.getCache("cacheA")).thenReturn(cacheA);

        when(cacheManager2.getCacheNames()).thenReturn(Set.of("cacheB"));
        when(cacheManager2.getCache("cacheB")).thenReturn(cacheB);

        jobExecution.setStatus(BatchStatus.COMPLETED);

        listener.afterJob(jobExecution);

        verify(cacheA).clear();
        verify(cacheB).clear();
    }

    @Test
    void afterJob_whenCacheIsNull_ignoresNullCache() {
        when(cacheManager1.getCacheNames()).thenReturn(Set.of("nullCache"));
        when(cacheManager1.getCache("nullCache")).thenReturn(null);

        jobExecution.setStatus(BatchStatus.COMPLETED);

        listener.afterJob(jobExecution);

        // Verification simply ensures no exceptions are thrown.
        verify(cacheManager1).getCache("nullCache");
    }

    @Test
    void afterJob_whenFailed_doesNotClearCaches() {
        jobExecution.setStatus(BatchStatus.FAILED);

        listener.afterJob(jobExecution);

        verifyNoInteractions(cacheManager1, cacheManager2, cacheA, cacheB);
    }
}
