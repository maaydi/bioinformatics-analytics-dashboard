package com.bioinformatics.dashboard.providers.uniprotkb.service;

import com.bioinformatics.dashboard.model.gene.GeneSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UniprotKbPaginationCacheService {
    private static final String CACHE_NAME = "uniprotCursors";
    private final CacheManager cacheManager;

    public String getCursorForRequest(GeneSearchRequest request) {
        if (request.page() == null || request.page() == 0) {
            return null;
        }

        return Objects.requireNonNull(cacheManager.getCache(CACHE_NAME)).get(request, String.class);
    }

    public void saveCursorForNextPage(GeneSearchRequest currentRequest, String nextCursor) {
        if (nextCursor == null || nextCursor.isBlank()) {
            return;
        }

        int nextPageNumber = (currentRequest.page() == null ? 0 : currentRequest.page()) + 1;
        GeneSearchRequest nextPageKey = currentRequest.withPage(nextPageNumber);

        Objects.requireNonNull(cacheManager.getCache(CACHE_NAME)).put(nextPageKey, nextCursor);
    }
}
