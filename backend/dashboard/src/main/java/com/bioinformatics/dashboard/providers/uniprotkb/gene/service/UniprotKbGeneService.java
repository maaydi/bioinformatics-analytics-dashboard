package com.bioinformatics.dashboard.providers.uniprotkb.gene.service;

import com.bioinformatics.common.exception.ExportRowCapExceededException;
import com.bioinformatics.common.exception.ResourceNotFoundException;
import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.common.providers.uniprotkb.service.UniprotKbPaginationCacheService;
import com.bioinformatics.common.uniprot.dto.UniProtEntry;
import com.bioinformatics.dashboard.config.AppProperties;
import com.bioinformatics.dashboard.csv.CsvWriter;
import com.bioinformatics.dashboard.interfaces.UniProtApiClient;
import com.bioinformatics.dashboard.interfaces.gene.GeneService;
import com.bioinformatics.dashboard.model.gene.PagedResponse;
import com.bioinformatics.dashboard.model.gene.ProteinDetailDto;
import com.bioinformatics.dashboard.model.gene.ProteinSummaryDto;
import com.bioinformatics.dashboard.providers.uniprotkb.AbstractUniprotKbProvider;
import com.bioinformatics.dashboard.providers.uniprotkb.mapper.UniProtProteinDtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Service for gene/protein operations.
 *
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UniprotKbGeneService extends AbstractUniprotKbProvider implements GeneService {

    private final AppProperties appProperties;
    private final UniProtApiClient client;
    private final UniprotKbPaginationCacheService cursorCacheService;
    private final UniProtProteinDtoMapper geneMapper;
    private final UniProtProteinDtoMapper mapper;

    /**
     * Returns a paginated, optionally sorted list of all proteins.
     *
     */
    @Override
    @Cacheable(value = "geneList-kb", key = "#pageNumber + '-' + #size + '-' + #sort + '-' + #direction")
    public PagedResponse<ProteinSummaryDto> listGenes(int pageNumber, int size, String sort, String direction) {
        var spec = GeneSearchRequest
                .builder()
                .page(pageNumber)
                .size(size)
                .sort(sort)
                .direction(direction)
                .build();
        var cursor = getCursor(spec);
        var result = client.fetchPage(spec, cursor);
        saveCursor(spec, result.nextCursor());
        var items = result.entries().stream().map(geneMapper::toSummary).toList();
        var totalPages = result.totalElements() / size;
        return new PagedResponse<>(items, pageNumber, size, result.totalElements(), (int) totalPages);
    }

    /**
     * Returns a paginated filtered result set.
     *
     */
    @Override
    @Cacheable(value = "geneSearch-kb", key = "#request.toString()")
    public PagedResponse<ProteinSummaryDto> searchGenes(GeneSearchRequest request) {
        log.info("Searching for protein entries for filters: {}", request);
        var cursor = getCursor(request);
        var result = client.fetchPage(request, cursor);
        saveCursor(request, result.nextCursor());
        var items = result.entries().stream().map(geneMapper::toSummary).toList();
        var totalPages = result.totalElements() / request.size();
        return new PagedResponse<>(items, request.page(), request.size(), result.totalElements(), (int) totalPages);

    }

    /**
     * Returns the full detail of a single protein entry.
     *
     * @throws ResourceNotFoundException if not found
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "geneDetail-kb", key = "#accession", cacheManager = "redisNonFinalAndRecordCacheManager")
    public ProteinDetailDto getGeneByAccession(String accession) {
        log.info("Retrieving protein entry by Accession: {}", accession);
        var spec = GeneSearchRequest
                .builder()
                .accession(accession)
                .build();
        var result = client.fetchPage(spec, null);
        var items = result.entries().stream().map(geneMapper::toDetail).toList();
        if (items.isEmpty()) {
            throw ResourceNotFoundException.forProtein(accession);
        }
        return items.getFirst();
    }

    /**
     * Streams all filtered rows as CSV into the provided writer.
     * Page configuration is ignored and all data are returned
     *
     */
    @Override
    public void exportCsv(GeneSearchRequest request, Writer writer, long totalRows) throws IOException {
        log.info("Exporting protein entries for filters: {}", request);
        var page = 0;
        var items = new ArrayList<UniProtEntry>();
        while (true) {
            var spec = request.copy().page(page).size(500).build();
            var cursor = getCursor(spec);
            var result = client.fetchPage(spec, cursor);
            items.addAll(result.entries());
            if (!result.hasMore()) break;
            page++;
            saveCursor(spec, result.nextCursor());
        }
        var csvWriter = new CsvWriter();
        csvWriter.write(writer, items.stream().map(mapper::toSummary).toList());
    }

    @Override
    public long assertWithinExportLimit(GeneSearchRequest request) {
        var maxSize = appProperties.getExport().getCsv().getMaxRows();
        var result = client.fetchPage(request, null);
        var totalRows = result.totalElements();
        if (totalRows > maxSize) {
            throw new ExportRowCapExceededException("Export limit exceeded. Result contains %d rows; maximum is %d. Please refine your filter"
                    .formatted(totalRows, maxSize));
        }
        return totalRows;

    }

    private String getCursor(GeneSearchRequest request) {
        var page = Objects.requireNonNullElse(request.page(), 0);
        if (page <= 0) {
            return null;
        }
        String cursor = null;
        int maxRetries = 5;
        while (maxRetries > 0) {
            cursor = cursorCacheService.getCursorForRequest(request);
            if (cursor != null) {
                break;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            maxRetries--;
        }
        if (cursor == null) {
            throw new IllegalArgumentException(
                    "Cannot skip to page " + page + ". You must fetch previous pages sequentially first."
            );
        }

        return cursor;
    }

    private void saveCursor(GeneSearchRequest request, String cursor) {
        cursorCacheService.saveCursorForNextPage(request, cursor);
    }
}
