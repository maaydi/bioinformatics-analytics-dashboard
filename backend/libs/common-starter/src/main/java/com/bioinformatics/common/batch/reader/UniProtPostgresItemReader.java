package com.bioinformatics.common.batch.reader;

import com.bioinformatics.common.gene.entity.ProteinEntry;
import com.bioinformatics.common.gene.service.ProteinEntryService;
import com.bioinformatics.common.gene.specification.GeneSpecification;
import com.bioinformatics.common.models.gene.GeneSearchRequest;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Spring Batch {@link ItemStreamReader} that reads {@link ProteinEntry} objects
 * from a PostgreSQL database using offset-based pagination, one page at a time.
 *
 * <h3>Pagination contract</h3>
 * <p>The reader uses standard offset-based pagination to query the database.
 * Each call to {@link #read()} checks if the internal buffer is empty;
 * if so, it fetches the next page using the current active page number.
 *
 * <ol>
 *   <li>On each call to {@link #read()}, if the internal buffer is empty and the
 *       source is not yet exhausted, the reader fetches the next page from
 *       the database via {@link ProteinEntryService#findAll(org.springframework.data.jpa.domain.Specification, org.springframework.data.domain.Pageable)}.</li>
 *   <li>Pages are fetched lazily: the reader never pre-fetches ahead of what the
 *       step needs.</li>
 *   <li>When the database returns no more results ({@code getTotalPages() <= activePage + 1}),
 *       the reader marks the source as exhausted and subsequent calls to {@link #read()} return
 *       {@code null} (end of stream).</li>
 * </ol>
 *
 * <h3>Restartability</h3>
 * The current page number is persisted in the Spring Batch
 * {@link ExecutionContext}. On a restart, the reader resumes exactly from the
 * page that was last checkpointed, without re-fetching earlier pages.
 *
 * <p><strong>Note:</strong> this reader is not thread-safe and is intended for
 * single-threaded step execution only.
 */
@Slf4j
public class UniProtPostgresItemReader implements ItemStreamReader<ProteinEntry> {

    private static final String PAGE_NUMBER = "uniProtPostgresItemReader.pageNumber";
    private static final String PAGE_COUNT_KEY = "uniProtPostgresItemReader.pageCount";

    private final ProteinEntryService proteinEntryService;
    private final GeneSearchRequest request;
    private final int requestPageSize;

    /**
     * In-memory buffer populated one page at a time.
     */
    private final Deque<ProteinEntry> buffer = new ArrayDeque<>();

    /**
     * Cursor to pass on the next API call. {@code null} triggers the first-page call.
     */
    private int activePage = -1;
    /**
     * Number of pages fetched so far (for logging / metrics only).
     */
    private int pageCount = 0;

    /**
     * Set to {@code true} once the API signals there are no more pages.
     */
    private boolean exhausted = false;

    public UniProtPostgresItemReader(ProteinEntryService service, GeneSearchRequest request, int requestPageSize) {
        this.proteinEntryService = service;
        this.request = request;
        this.requestPageSize = requestPageSize;
    }


    /**
     * Restores cursor state from a previous execution (restart scenario).
     * On a fresh start the context is empty and the reader begins at the first page.
     */
    @Override
    public void open(ExecutionContext executionContext) {
        if (executionContext.containsKey(PAGE_NUMBER)) {
            activePage = executionContext.getInt(PAGE_NUMBER);
            pageCount = executionContext.getInt(PAGE_COUNT_KEY, 0);
            log.info("UniProtPostgresItemReader restarting from page={} (pageCount={})", activePage, pageCount);
        } else {
            log.info("UniProtPostgresItemReader starting from the first page");
        }
    }

    /**
     * Persists the current active page so the step can restart from the last
     * successfully committed chunk.
     */
    @Override
    public void update(@NonNull ExecutionContext executionContext) {
        if (activePage != -1) {
            executionContext.putInt(PAGE_NUMBER, activePage);
        }
        executionContext.putInt(PAGE_COUNT_KEY, pageCount);
    }

    /**
     * Clears the in-memory buffer on step completion or failure.
     */
    @Override
    public void close() {
        buffer.clear();
        log.debug("UniProtPostgresItemReader closed after {} page(s)", pageCount);
    }


    /**
     * Returns the next {@link ProteinEntry}, fetching a new page from the database
     * when the buffer is empty.
     *
     * @return the next entry, or {@code null} when all pages have been consumed
     */
    @Override
    public ProteinEntry read() {
        if (buffer.isEmpty() && !exhausted) {
            loadNextPage();
        }
        return buffer.isEmpty() ? null : buffer.poll();
    }


    private void loadNextPage() {
        log.debug("Fetching UniProt Postgres page {} (size {})", activePage, requestPageSize);
        var page = PageRequest.of(activePage, requestPageSize);
        var spec = GeneSpecification.fromRequest(request);
        var genes = proteinEntryService.findAll(spec, page);
        var entries = genes.getContent();
        buffer.addAll(entries);
        exhausted = !genes.hasNext();
        activePage++;
        pageCount++;
        log.info("Fetched page {} — {} entries, hasMore={}", activePage, entries.size(), !exhausted);
    }
}

