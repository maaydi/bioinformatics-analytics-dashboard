package com.bioinformatics.dashboard.providers.dispatcher;

import com.bioinformatics.dashboard.interfaces.gene.GeneService;
import com.bioinformatics.dashboard.model.gene.GeneSearchRequest;
import com.bioinformatics.dashboard.model.gene.PagedResponse;
import com.bioinformatics.dashboard.model.gene.ProteinDetailDto;
import com.bioinformatics.dashboard.model.gene.ProteinSummaryDto;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Writer;
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
     * Delegate CSV export to active provider.
     */
    @Override
    public void exportCsv(GeneSearchRequest request, Writer writer, long totalRows) throws IOException {
        resolve().exportCsv(request, writer, totalRows);
    }

    /**
     * Delegate export limit assertion to active provider.
     */
    @Override
    public long assertWithinExportLimit(GeneSearchRequest request) {
        return resolve().assertWithinExportLimit(request);
    }

}
