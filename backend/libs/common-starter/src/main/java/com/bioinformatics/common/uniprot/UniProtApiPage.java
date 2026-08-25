package com.bioinformatics.common.uniprot;


import com.bioinformatics.common.uniprot.dto.UniProtEntry;

import java.util.List;

/**
 * Represents a single page returned by the UniProt API client.
 *
 * @param entries    the list of entries on this page (never null, may be empty)
 * @param hasMore    {@code true} when a subsequent page exists and should be fetched
 * @param nextCursor opaque cursor value extracted from the {@code Link} response header;
 *                   {@code null} when this is the last page
 */
public record UniProtApiPage(List<UniProtEntry> entries, boolean hasMore, String nextCursor, long totalElements) {

    /**
     * Convenience factory for a terminal (last) page.
     */
    public static UniProtApiPage lastPage(List<UniProtEntry> entries, long totalElements) {
        return new UniProtApiPage(entries, false, null, totalElements);
    }

    /**
     * Convenience factory for a non-terminal page.
     *
     * @param entries    entries on this page
     * @param nextCursor cursor to pass on the next API call
     */
    public static UniProtApiPage nextPage(List<UniProtEntry> entries, String nextCursor, long totalElements) {
        return new UniProtApiPage(entries, true, nextCursor, totalElements);
    }
}

