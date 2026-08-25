package com.bioinformatics.importservice.batch.processor.resolver;

import com.bioinformatics.common.gene.service.ProteinEntryService;
import com.bioinformatics.importservice.resolver.ProteinAccessionResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProteinAccessionResolverTest {

    @Mock
    private ProteinEntryService proteinEntryService;

    @Test
    void initLoadsAccessionsFromService() {
        when(proteinEntryService.findAllAccessions()).thenReturn(Arrays.asList("P1", "P2"));

        var resolver = new ProteinAccessionResolver(proteinEntryService);
        resolver.init();

        assertTrue(resolver.alreadyExists("P1"), "P1 should already exist after init");
        assertTrue(resolver.alreadyExists("P2"), "P2 should already exist after init");

        assertFalse(resolver.alreadyExists("P3"), "P3 should not exist before being added");
    }

    @Test
    void alreadyExistsForNewThenExisting() {
        when(proteinEntryService.findAllAccessions()).thenReturn(Collections.emptyList());

        var resolver = new ProteinAccessionResolver(proteinEntryService);
        resolver.init();

        assertFalse(resolver.alreadyExists("NEW"));
        assertTrue(resolver.alreadyExists("NEW"));
    }

    @Test
    void initThrowsWhenServiceReturnsNull() {
        when(proteinEntryService.findAllAccessions()).thenReturn(null);

        var resolver = new ProteinAccessionResolver(proteinEntryService);
        assertThrows(NullPointerException.class, resolver::init);
    }

    @Test
    void concurrentAddsOnlyOneWins() throws InterruptedException {
        when(proteinEntryService.findAllAccessions()).thenReturn(Collections.emptyList());

        var resolver = new ProteinAccessionResolver(proteinEntryService);
        resolver.init();

        final int threads = 20;
        try (var exec = Executors.newFixedThreadPool(threads)) {
            var start = new CountDownLatch(1);
            var firstFalse = new AtomicInteger(0);
            var trueCount = new AtomicInteger(0);

            var futures = new ArrayList<Future<?>>();
            for (int i = 0; i < threads; i++) {
                futures.add(exec.submit(() -> {
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    var existed = resolver.alreadyExists("CONCUR");
                    if (existed) {
                        trueCount.incrementAndGet();
                    } else {
                        firstFalse.incrementAndGet();
                    }
                }));
            }
            // Start all threads
            start.countDown();
            for (var f : futures) {
                try {
                    f.get();
                } catch (ExecutionException e) {
                    fail(e);
                }
            }
            exec.shutdown();
            assertEquals(1, firstFalse.get(), "Exactly one thread should add the accession");
            assertEquals(threads - 1, trueCount.get(), "Remaining threads should observe it already existed");
        }
    }
}

