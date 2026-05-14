package com.bioinformatics.dashboard.gene.service;

import com.bioinformatics.dashboard.exception.ResourceNotFoundException;
import com.bioinformatics.dashboard.gene.dto.GeneSearchRequest;
import com.bioinformatics.dashboard.gene.dto.PagedResponse;
import com.bioinformatics.dashboard.gene.dto.ProteinDetailDto;
import com.bioinformatics.dashboard.gene.dto.ProteinSummaryDto;
import com.bioinformatics.dashboard.gene.mapper.GeneMapper;
import com.bioinformatics.dashboard.gene.repository.ProteinEntryRepository;
import com.bioinformatics.dashboard.gene.specification.GeneSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        var direct = Sort.Direction.fromString(request.direction());
        var page = PageRequest.of(request.page(), request.size(), direct, request.sort());
        var spec = GeneSpecification.fromRequest(request);
        var result = repository.findAll(spec, page);
        var genes = result.getContent().stream().map(mapper::toSummary).toList();
        return new PagedResponse<>(genes, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());

    }

    /**
     * Returns the full detail of a single protein entry.
     *
     * @throws com.bioinformatics.dashboard.exception.ResourceNotFoundException if not found
     * @see documentation/api-contract.md — GET /api/genes/{id}
     */
    public ProteinDetailDto getGeneById(Long id) {
        var gene = repository.findById(id).orElseThrow(() -> ResourceNotFoundException.forProtein(id));
        return mapper.toDetail(gene);

    } // Replace Object with ProteinDetailDto when implemented

    /**
     * Streams all filtered rows as CSV into the provided writer.
     *
     * @see documentation/api-contract.md — POST /api/genes/export-csv
     */
    public void exportCsv(GeneSearchRequest request, java.io.Writer writer) {

    }
}
