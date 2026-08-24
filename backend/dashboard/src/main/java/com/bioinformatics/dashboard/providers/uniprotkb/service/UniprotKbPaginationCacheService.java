package com.bioinformatics.dashboard.providers.uniprotkb.service;

import com.bioinformatics.common.models.gene.GeneSearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Cache management service for UniProt KB cursor-based pagination.
 *
 * <p>Maintains an in-memory cache of pagination cursors keyed by {@link GeneSearchRequest},
 * enabling seamless continuation of multi-page searches. The UniProt API uses opaque cursor
 * tokens (rather than offset-based pagination) to navigate result sets, and this service
 * persists those cursors for the next page lookup.</p>
 *
 * <p>Design note: Cursors are only retrieved for explicitly requested pages (page &gt; 0);
 * the first page (page 0 or null) uses no cursor.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UniprotKbPaginationCacheService {
    private static final String CACHE_NAME = "uniprotCursors";
    private final CacheManager cacheManager;

    /**
     * Retrieves a stored cursor for advancing to the specified page.
     *
     * @param request the search request, which may include a non-zero page number
     * @return the cached cursor string for that page, or {@code null} if not found or if the page is 0/null
     */
    public String getCursorForRequest(GeneSearchRequest request) {
        if (request.page() == null || request.page() == 0) {
            return null;
        }

        return Objects.requireNonNull(cacheManager.getCache(CACHE_NAME)).get(request, String.class);
    }

    /**
     * Stores a cursor for the next page of results.
     *
     * <p>Automatically increments the page number of the current request to compute
     * the key for caching the next cursor.</p>
     *
     * @param currentRequest the current search request
     * @param nextCursor     the cursor to use for advancing to the next page;
     *                       ignored if {@code null} or empty
     */
    public void saveCursorForNextPage(GeneSearchRequest currentRequest, String nextCursor) {
        if (nextCursor == null || nextCursor.isBlank()) {
            return;
        }

        var nextPageNumber = (currentRequest.page() == null ? 0 : currentRequest.page()) + 1;
        var nextPageKey = currentRequest.copy().page(nextPageNumber).build();
        Objects.requireNonNull(cacheManager.getCache(CACHE_NAME)).put(nextPageKey, nextCursor);
    }
}
