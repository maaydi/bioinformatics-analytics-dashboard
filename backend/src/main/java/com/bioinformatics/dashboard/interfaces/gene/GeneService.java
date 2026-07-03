package com.bioinformatics.dashboard.interfaces.gene;

import com.bioinformatics.dashboard.interfaces.Provider;
import com.bioinformatics.dashboard.model.gene.GeneSearchRequest;
import com.bioinformatics.dashboard.model.gene.PagedResponse;
import com.bioinformatics.dashboard.model.gene.ProteinDetailDto;
import com.bioinformatics.dashboard.model.gene.ProteinSummaryDto;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Provider contract for gene/protein data operations.
 * Implementations must support pagination, filtering, detail retrieval, CSV export, and keyword listing.
 */
public interface GeneService extends Provider {

    /**
     * Fetch paginated list of all genes with optional sorting.
     *
     * @param pageNumber zero-based page index
     * @param size       rows per page
     * @param sort       field name to sort by
     * @param direction  "ASC" or "DESC"
     * @return paginated genes summary
     */
    PagedResponse<ProteinSummaryDto> listGenes(int pageNumber, int size, String sort, String direction);

    /**
     * Search genes with dynamic filters (accession, keyword, GO term, etc.).
     * @param request filter and pagination parameters
     * @return paginated search results
     */
    PagedResponse<ProteinSummaryDto> searchGenes(GeneSearchRequest request);

    /**
     * Fetch full details of a single gene by ID.
     * @param id protein entry ID
     * @return gene details with all related data
     */
    ProteinDetailDto getGeneById(Long id);

    /**
     * Export filtered genes as CSV to the provided writer.
     * @param request search/filter criteria
     * @param writer output destination
     * @param totalRows total count of rows to export
     * @throws IOException on write error
     */
    void exportCsv(GeneSearchRequest request, Writer writer, long totalRows) throws IOException;

    /**
     * Validate export size against configured limit.
     * @param request search/filter criteria
     * @return total row count if within limit
     */
    long assertWithinExportLimit(GeneSearchRequest request);

    /**
     * Fetch all available protein keywords/tags.
     * @return list of unique keywords
     */
    List<String> listKeywords();
}
