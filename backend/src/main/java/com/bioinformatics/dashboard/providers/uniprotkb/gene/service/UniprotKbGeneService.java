package com.bioinformatics.dashboard.providers.uniprotkb.gene.service;

import com.bioinformatics.dashboard.config.AppProperties;
import com.bioinformatics.dashboard.exception.ExportRowCapExceededException;
import com.bioinformatics.dashboard.exception.ResourceNotFoundException;
import com.bioinformatics.dashboard.interfaces.gene.GeneService;
import com.bioinformatics.dashboard.model.gene.GeneSearchRequest;
import com.bioinformatics.dashboard.model.gene.PagedResponse;
import com.bioinformatics.dashboard.model.gene.ProteinDetailDto;
import com.bioinformatics.dashboard.model.gene.ProteinSummaryDto;
import com.bioinformatics.dashboard.providers.postgres.gene.specification.GeneSpecification;
import com.bioinformatics.dashboard.providers.uniprotkb.AbstractUniprotKbProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Service for gene/protein operations.
 *
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UniprotKbGeneService extends AbstractUniprotKbProvider implements GeneService {

    private final AppProperties appProperties;

    /**
     * Returns a paginated, optionally sorted list of all proteins.
     *
     */
    @Override
    @Cacheable(value = "geneList", key = "#pageNumber + '-' + #size + '-' + #sort + '-' + #direction")
    public PagedResponse<ProteinSummaryDto> listGenes(int pageNumber, int size, String sort, String direction) {
        // TODO implement it
        return PagedResponse.of(null);
    }

    /**
     * Returns a paginated filtered result set.
     *
     */
    @Override
    @Cacheable(value = "geneSearch", key = "#request.toString()")
    public PagedResponse<ProteinSummaryDto> searchGenes(GeneSearchRequest request) {
        log.info("Searching for protein entries for filters: {}", request);
        // TODO implement it
        return PagedResponse.of(null);

    }

    /**
     * Returns the full detail of a single protein entry.
     *
     * @throws ResourceNotFoundException if not found
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "geneDetail", key = "#id", cacheManager = "redisNonFinalAndRecordCacheManager")
    public ProteinDetailDto getGeneById(Long id) {
        log.info("Retrieving protein entry by id: {}", id);
        // TODO implement it
        return null;
    }

    /**
     * Streams all filtered rows as CSV into the provided writer.
     * Page configuration is ignored and all data are returned
     *
     */
    @Override
    public void exportCsv(GeneSearchRequest request, Writer writer, long totalRows) throws IOException {
        log.info("Exporting protein entries for filters: {}", request);
        // TODO implement it

    }

    @Override
    public long assertWithinExportLimit(GeneSearchRequest request) {
        var maxSize = appProperties.getExport().getCsv().getMaxRows();
        var spec = GeneSpecification.fromRequest(request);
        var totalRows = 500;// proteinService.count(spec);
        if (totalRows > maxSize) {
            throw new ExportRowCapExceededException("Export limit exceeded. Result contains %d rows; maximum is %d. Please refine your filter"
                    .formatted(totalRows, maxSize));
        }
        return totalRows;

    }

    @Override
    @Cacheable(value = "geneKeywords")
    public List<String> listKeywords() {
        log.info("Retrieving keywords for protein entries");
        // TODO implement it
        return List.of();
    }
}
