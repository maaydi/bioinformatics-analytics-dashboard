package com.bioinformatics.dashboard.interfaces.gene;

import com.bioinformatics.dashboard.interfaces.Provider;
import com.bioinformatics.dashboard.model.gene.GeneSearchRequest;
import com.bioinformatics.dashboard.model.gene.PagedResponse;
import com.bioinformatics.dashboard.model.gene.ProteinDetailDto;
import com.bioinformatics.dashboard.model.gene.ProteinSummaryDto;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public interface GeneService extends Provider {
    PagedResponse<ProteinSummaryDto> listGenes(int pageNumber, int size, String sort, String direction);

    PagedResponse<ProteinSummaryDto> searchGenes(GeneSearchRequest request);

    ProteinDetailDto getGeneById(Long id);

    void exportCsv(GeneSearchRequest request, Writer writer, long totalRows) throws IOException;

    long assertWithinExportLimit(GeneSearchRequest request);

    List<String> listKeywords();
}
