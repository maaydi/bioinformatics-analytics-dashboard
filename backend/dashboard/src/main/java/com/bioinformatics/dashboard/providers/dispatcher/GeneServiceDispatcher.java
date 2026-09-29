package com.bioinformatics.dashboard.providers.dispatcher;

import com.bioinformatics.common.gene.dto.ProteinDetailDto;
import com.bioinformatics.common.gene.dto.ProteinSummaryDto;
import com.bioinformatics.common.models.PagedResponse;
import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.common.providers.AbstractProviderDispatcher;
import com.bioinformatics.dashboard.interfaces.gene.GeneService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Dispatches gene-related requests to the active provider implementation.
 *
 * <p>Each concrete {@link GeneService} implementation exposes a provider key and is resolved dynamically from the
 * current request context. This keeps the controller API stable while allowing the underlying data source to vary,
 * for example between PostgreSQL and a UniProt-backed service.</p>
 */
@Service
@Primary
@Slf4j
public class GeneServiceDispatcher extends AbstractProviderDispatcher<GeneService> implements GeneService {

    /**
     * Creates the dispatcher with all available gene-service implementations.
     *
     * @param services list of gene-provider beans registered in the Spring context
     */
    public GeneServiceDispatcher(List<GeneService> services) {
        super(services);
        log.info("[DASHBOARD][GENE_DISPATCHER] Initialized gene provider registry size={}", services.size());
    }

    /**
     * Delegates the list operation to the currently active provider.
     *
     * @param pageNumber zero-based page index
     * @param size rows per page
     * @param sort sort field name
     * @param direction sort direction
     * @return provider-specific paginated gene summary response
     */
    @Override
    public PagedResponse<ProteinSummaryDto> listGenes(int pageNumber, int size, String sort, String direction) {
        log.debug("[DASHBOARD][GENE_DISPATCHER] Forwarding gene list request page={} size={} sort={} direction={}", pageNumber, size, sort, direction);
        return resolve().listGenes(pageNumber, size, sort, direction);
    }

    /**
     * Delegates the filtered search operation to the currently active provider.
     *
     * @param request full search criteria
     * @return paginated search result set
     */
    @Override
    public PagedResponse<ProteinSummaryDto> searchGenes(GeneSearchRequest request) {
        log.debug("[DASHBOARD][GENE_DISPATCHER] Forwarding gene search request={}", request);
        return resolve().searchGenes(request);
    }

    /**
     * Delegates detailed lookup to the active provider.
     *
     * @param accession protein accession to resolve
     * @return full protein detail object
     */
    @Override
    public ProteinDetailDto getGeneByAccession(String accession) {
        log.debug("[DASHBOARD][GENE_DISPATCHER] Forwarding gene detail request accession={}", accession);
        return resolve().getGeneByAccession(accession);
    }

    /**
     * Delegates count operations to the currently active provider.
     *
     * @param request filter request for count calculation
     * @return number of matching rows
     */
    @Override
    public long count(GeneSearchRequest request) {
        log.debug("[DASHBOARD][GENE_DISPATCHER] Forwarding gene count request={}", request);
        return resolve().count(request);
    }
}
