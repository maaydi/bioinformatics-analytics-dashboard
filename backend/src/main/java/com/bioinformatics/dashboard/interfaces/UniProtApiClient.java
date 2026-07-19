package com.bioinformatics.dashboard.interfaces;


import com.bioinformatics.dashboard.model.uniprot.UniProtApiPage;

/**
 * Abstraction for a paginated UniProt API data source using cursor-based pagination.
 *
 * <p>The UniProtKB REST API exposes cursor navigation via a {@code Link} response header.
 * Callers drive pagination by passing the cursor returned in the previous page; when
 * {@link UniProtApiPage#hasMore()} returns {@code false} no further calls should be made.
 *
 * <p>Implementations must be thread-safe if used in a partitioned step;
 * the default single-threaded step does not require thread-safety.
 */
public interface UniProtApiClient {

    /**
     * Fetches one page of UniProt entries.
     *
     * @param cursor   opaque cursor value from the previous page's {@code Link} header;
     *                 pass {@code null} to start from the first page
     * @param pageSize maximum number of entries to return per page (1–500)
     * @return a page of entries, a flag indicating whether more pages follow,
     * and the cursor to use for the next call
     */
    UniProtApiPage fetchPage(String cursor, int pageSize);
}

