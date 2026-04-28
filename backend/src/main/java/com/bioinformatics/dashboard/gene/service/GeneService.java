package com.bioinformatics.dashboard.gene.service;

import com.bioinformatics.dashboard.gene.dto.GeneSearchRequest;
import com.bioinformatics.dashboard.gene.dto.PagedResponse;
import com.bioinformatics.dashboard.gene.dto.ProteinSummaryDto;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for gene/protein operations.
 *
 * <p>All business logic lives here; the controller delegates to this interface.
 * Implementation must validate cross-field rules not covered by Bean Validation
 * (e.g. lengthMin ≤ lengthMax — see documentation/validation-rules.md §2).
 */
public interface GeneService {

    /**
     * Returns a paginated, optionally sorted list of all proteins.
     *
     * @see documentation/api-contract.md — GET /api/genes
     */
    PagedResponse<ProteinSummaryDto> listGenes(Pageable pageable);

    /**
     * Returns a paginated filtered result set.
     *
     * @see documentation/api-contract.md — POST /api/genes/search
     */
    PagedResponse<ProteinSummaryDto> searchGenes(GeneSearchRequest request);

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
