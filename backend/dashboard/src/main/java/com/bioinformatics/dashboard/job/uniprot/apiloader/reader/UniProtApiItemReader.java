package com.bioinformatics.dashboard.job.uniprot.apiloader.reader;

import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.common.uniprot.UniProtApiPage;
import com.bioinformatics.common.uniprot.dto.UniProtEntry;
import com.bioinformatics.dashboard.interfaces.UniProtApiClient;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Spring Batch {@link ItemStreamReader} that reads {@link UniProtEntry} objects
 * by calling the UniProt API sequentially, one page at a time.
 *
 * <h3>Pagination contract</h3>
 * <p>UniProtKB uses opaque cursor-based pagination exposed via the {@code Link}
 * response header. The reader passes {@code null} on the first call and then
 * threads the cursor returned by each {@link UniProtApiPage} into the next call.
 *
 * <ol>
 *   <li>On each call to {@link #read()}, if the internal buffer is empty and the
 *       source is not yet exhausted, the reader fetches the next page from
 *       {@link UniProtApiClient#fetchPage(com.bioinformatics.common.models.gene.GeneSearchRequest, String)}.</li>
 *   <li>Pages are fetched lazily: the reader never pre-fetches ahead of what the
 *       step needs.</li>
 *   <li>When {@link UniProtApiPage#hasMore()} returns {@code false}, no further
 *       API calls are made and subsequent calls to {@link #read()} return
 *       {@code null} (end of stream).</li>
 * </ol>
 *
 * <h3>Restartability</h3>
 * The cursor of the last committed page is persisted in the Spring Batch
 * {@link ExecutionContext}. On a restart, the reader resumes exactly from the
 * cursor that was last checkpointed, without re-fetching earlier pages.
 *
 * <p><strong>Note:</strong> this reader is not thread-safe and is intended for
 * single-threaded step execution only.
 */
@Slf4j
public class UniProtApiItemReader implements ItemStreamReader<UniProtEntry> {

    private static final String CURSOR_KEY = "uniProtApiItemReader.nextCursor";
    private static final String PAGE_COUNT_KEY = "uniProtApiItemReader.pageCount";

    private final UniProtApiClient apiClient;
    private final GeneSearchRequest request;

    /**
     * In-memory buffer populated one page at a time.
     */
    private final Deque<UniProtEntry> buffer = new ArrayDeque<>();

    /**
     * Cursor to pass on the next API call. {@code null} triggers the first-page call.
     */
    private String nextCursor = null;

    /**
     * Number of pages fetched so far (for logging / metrics only).
     */
    private int pageCount = 0;

    /**
     * Set to {@code true} once the API signals there are no more pages.
     */
    private boolean exhausted = false;

    public UniProtApiItemReader(UniProtApiClient apiClient, GeneSearchRequest request) {
        this.apiClient = apiClient;
        this.request = request;
    }


    /**
     * Restores cursor state from a previous execution (restart scenario).
     * On a fresh start the context is empty and the reader begins at the first page.
     */
    @Override
    public void open(ExecutionContext executionContext) {
        if (executionContext.containsKey(CURSOR_KEY)) {
            nextCursor = executionContext.getString(CURSOR_KEY);
            pageCount = executionContext.getInt(PAGE_COUNT_KEY, 0);
            log.info("UniProtApiItemReader restarting from cursor={} (pageCount={})", nextCursor, pageCount);
        } else {
            log.info("UniProtApiItemReader starting from the first page");
        }
    }

    /**
     * Persists the current cursor so the step can restart from the last
     * successfully committed chunk.
     */
    @Override
    public void update(@NonNull ExecutionContext executionContext) {
        if (nextCursor != null) {
            executionContext.putString(CURSOR_KEY, nextCursor);
        }
        executionContext.putInt(PAGE_COUNT_KEY, pageCount);
    }

    /**
     * Clears the in-memory buffer on step completion or failure.
     */
    @Override
    public void close() {
        buffer.clear();
        log.debug("UniProtApiItemReader closed after {} page(s)", pageCount);
    }


    /**
     * Returns the next {@link UniProtEntry}, fetching a new page from the API
     * when the buffer is empty.
     *
     * @return the next entry, or {@code null} when all pages have been consumed
     */
    @Override
    public UniProtEntry read() {
        if (buffer.isEmpty() && !exhausted) {
            loadNextPage();
        }
        return buffer.isEmpty() ? null : buffer.poll();
    }


    private void loadNextPage() {
        log.debug("Fetching UniProt API page {} (cursor={}, pageSize={})", pageCount, nextCursor, request.size());
        var page = apiClient.fetchPage(request, nextCursor);
        var entries = page.entries();

        buffer.addAll(entries);
        exhausted = !page.hasMore();
        nextCursor = page.nextCursor();
        pageCount++;

        log.info("Fetched page {} — {} entries, hasMore={}", pageCount, entries.size(), page.hasMore());
    }
}

