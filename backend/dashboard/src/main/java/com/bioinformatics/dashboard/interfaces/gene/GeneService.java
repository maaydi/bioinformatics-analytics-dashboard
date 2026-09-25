package com.bioinformatics.dashboard.interfaces.gene;

import com.bioinformatics.common.gene.dto.ProteinDetailDto;
import com.bioinformatics.common.gene.dto.ProteinSummaryDto;
import com.bioinformatics.common.models.PagedResponse;
import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.common.providers.Provider;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provider contract for gene/protein data operations.
 * Implementations must support pagination, filtering, detail retrieval, CSV export, and keyword listing.
 */
public interface GeneService extends Provider {

    Set<String> SORT_WHITELIST = Arrays.stream(ProteinSummaryDto.class.getDeclaredFields())
            .map(Field::getName).collect(Collectors.toSet());

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
     *
     * @param accession protein entry accession
     * @return gene details with all related data
     */
    ProteinDetailDto getGeneByAccession(String accession);

    ;

    /**
     * Count total rows for a given Search request
     *
     * @param request search/filter criteria
     * @return total row count
     */
    long count(GeneSearchRequest request);

}
