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

@Service
@Primary
public class GeneServiceDispatcher extends AbstractProviderDispatcher<GeneService> implements GeneService {

    public GeneServiceDispatcher(List<GeneService> services) {
        super(services);
    }


    @Override
    public PagedResponse<ProteinSummaryDto> listGenes(int pageNumber, int size, String sort, String direction) {
        return resolve().listGenes(pageNumber, size, sort, direction);
    }

    @Override
    public PagedResponse<ProteinSummaryDto> searchGenes(GeneSearchRequest request) {
        return resolve().searchGenes(request);
    }

    @Override
    public ProteinDetailDto getGeneById(Long id) {
        return resolve().getGeneById(id);
    }

    @Override
    public void exportCsv(GeneSearchRequest request, Writer writer, long totalRows) throws IOException {
        resolve().exportCsv(request, writer, totalRows);
    }

    @Override
    public long assertWithinExportLimit(GeneSearchRequest request) {
        return resolve().assertWithinExportLimit(request);
    }

    @Override
    public List<String> listKeywords() {
        return resolve().listKeywords();
    }
}
