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
 * Contract for gene and protein query operations exposed by the dashboard package.
 *
 * <p>This interface abstracts the retrieval model used by the presentation layer and allows the actual data source
 * to vary by provider. Implementations may read from PostgreSQL-backed protein tables or from external UniProt APIs,
 * but callers observe a single, consistent gene API.</p>
 */
public interface GeneService extends Provider {

    Set<String> SORT_WHITELIST = Arrays.stream(ProteinSummaryDto.class.getDeclaredFields())
            .map(Field::getName).collect(Collectors.toSet());

    /**
     * Fetches a paginated list of protein summaries.
     *
     * @param pageNumber zero-based page index
     * @param size rows per page
     * @param sort field used to sort the dataset
     * @param direction ASC or DESC ordering
     * @return paginated summary payload
     */
    PagedResponse<ProteinSummaryDto> listGenes(int pageNumber, int size, String sort, String direction);

    /**
     * Executes a dynamic gene search based on the supplied request.
     *
     * @param request structured filter and pagination criteria
     * @return paginated result set matching the request
     */
    PagedResponse<ProteinSummaryDto> searchGenes(GeneSearchRequest request);

    /**
     * Fetches the full detail of a single protein by accession.
     *
     * @param accession UniProt accession identifier
     * @return complete protein detail payload
     */
    ProteinDetailDto getGeneByAccession(String accession);

    /**
     * Counts the number of records matching the supplied search request.
     *
     * @param request filter criteria for the count operation
     * @return number of matching rows
     */
    long count(GeneSearchRequest request);
}
