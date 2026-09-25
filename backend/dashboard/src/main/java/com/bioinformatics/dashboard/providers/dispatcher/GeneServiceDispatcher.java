package com.bioinformatics.dashboard.providers.dispatcher;

import com.bioinformatics.common.gene.dto.ProteinDetailDto;
import com.bioinformatics.common.gene.dto.ProteinSummaryDto;
import com.bioinformatics.common.models.PagedResponse;
import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.common.providers.AbstractProviderDispatcher;
import com.bioinformatics.dashboard.interfaces.gene.GeneService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Dispatcher for GeneService implementations.
 * Routes all gene operations to the active provider based on ProviderContextHolder.
 * Marked as @Primary so controllers inject this dispatcher instead of concrete implementations.
 */
@Service
@Primary
public class GeneServiceDispatcher extends AbstractProviderDispatcher<GeneService> implements GeneService {

    /**
     * Initialize dispatcher with all registered GeneService implementations.
     *
     * @param services all GeneService beans (postgres, mongo, rdf, etc.)
     */
    public GeneServiceDispatcher(List<GeneService> services) {
        super(services);
    }

    /**
     * Delegate listGenes to active provider.
     */
    @Override
    public PagedResponse<ProteinSummaryDto> listGenes(int pageNumber, int size, String sort, String direction) {
        return resolve().listGenes(pageNumber, size, sort, direction);
    }

    /**
     * Delegate searchGenes to active provider.
     */
    @Override
    public PagedResponse<ProteinSummaryDto> searchGenes(GeneSearchRequest request) {
        return resolve().searchGenes(request);
    }

    /**
     * Delegate getGeneById to active provider.
     */
    @Override
    public ProteinDetailDto getGeneByAccession(String accession) {
        return resolve().getGeneByAccession(accession);
    }

    /**
     * Delegate export limit assertion to active provider.
     */
    @Override
    public long count(GeneSearchRequest request) {
        return resolve().count(request);
    }

}
