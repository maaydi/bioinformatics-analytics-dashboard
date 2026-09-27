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
 * from PostgreSQL using offset-based pagination.
 *
 * <p>The reader lazily fetches page-sized database chunks and persists the current page number in the
 * execution context so an interrupted job can resume without re-reading earlier results.
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
     * Current page index used for the next database call.
     */
    private int activePage = -1;

    /**
     * Number of pages fetched so far (for logging / metrics only).
     */
    private int pageCount = 0;

    /**
     * Set to {@code true} once there are no more pages.
     */
    private boolean exhausted = false;

    public UniProtPostgresItemReader(ProteinEntryService service, GeneSearchRequest request, int requestPageSize) {
        this.proteinEntryService = service;
        this.request = request;
        this.requestPageSize = requestPageSize;
    }

    /**
     * Restores page state from a previous execution (restart scenario).
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
     * Persists the current page state so the step can resume from the last committed chunk.
     */
    @Override
    public void update(@NonNull ExecutionContext executionContext) {
        if (activePage != -1) {
            executionContext.putInt(PAGE_NUMBER, activePage);
        }
        executionContext.putInt(PAGE_COUNT_KEY, pageCount);
        log.debug("UniProtPostgresItemReader checkpointed page={} pageCount={}", activePage, pageCount);
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
     * Returns the next {@link ProteinEntry}, fetching a new page when the buffer is empty.
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
