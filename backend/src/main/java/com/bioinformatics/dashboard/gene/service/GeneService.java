package com.bioinformatics.dashboard.gene.service;

import com.bioinformatics.dashboard.gene.dto.GeneSearchRequest;
import com.bioinformatics.dashboard.gene.dto.PagedResponse;
import com.bioinformatics.dashboard.gene.dto.ProteinSummaryDto;
import com.bioinformatics.dashboard.gene.mapper.GeneMapper;
import com.bioinformatics.dashboard.gene.repository.ProteinEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Service for gene/protein operations.
 *
 */
@Component
@RequiredArgsConstructor
public class GeneService {

    private final ProteinEntryRepository repository;
    private final GeneMapper mapper;

    /**
     * Returns a paginated, optionally sorted list of all proteins.
     *
     * @see documentation/api-contract.md — GET /api/genes
     */
    public PagedResponse<ProteinSummaryDto> listGenes(Pageable pageable) {
        var page = repository.findAll(pageable);
        var genes = page.getContent().stream().map(mapper::toSummary).toList();
        return new PagedResponse<>(genes, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    /**
     * Returns a paginated filtered result set.
     *
     * @see documentation/api-contract.md — POST /api/genes/search
     */
    public PagedResponse<ProteinSummaryDto> searchGenes(GeneSearchRequest request) {
    }

    /**
     * Returns the full detail of a single protein entry.
     *
     * @throws com.bioinformatics.dashboard.exception.ResourceNotFoundException if not found
     * @see documentation/api-contract.md — GET /api/genes/{id}
     */
    Object getGeneById(Long id); // Replace Object with ProteinDetailDto when implemented

    /**
     * Streams all filtered rows as CSV into the provided writer.
     *
     * @see documentation/api-contract.md — POST /api/genes/export-csv
     */
    void exportCsv(GeneSearchRequest request, java.io.Writer writer);
}
