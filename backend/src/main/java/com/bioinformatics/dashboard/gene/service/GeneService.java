package com.bioinformatics.dashboard.gene.service;

import com.bioinformatics.dashboard.config.AppProperties;
import com.bioinformatics.dashboard.csv.CsvWriter;
import com.bioinformatics.dashboard.exception.PayloadTooLargeException;
import com.bioinformatics.dashboard.exception.ResourceNotFoundException;
import com.bioinformatics.dashboard.gene.dto.GeneSearchRequest;
import com.bioinformatics.dashboard.gene.dto.PagedResponse;
import com.bioinformatics.dashboard.gene.dto.ProteinDetailDto;
import com.bioinformatics.dashboard.gene.dto.ProteinSummaryDto;
import com.bioinformatics.dashboard.gene.entity.Keyword;
import com.bioinformatics.dashboard.gene.mapper.GeneMapper;
import com.bioinformatics.dashboard.gene.repository.KeywordRepository;
import com.bioinformatics.dashboard.gene.repository.ProteinEntryRepository;
import com.bioinformatics.dashboard.gene.specification.GeneSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for gene/protein operations.
 *
 */
@Component
@RequiredArgsConstructor
public class GeneService {

    private final ProteinEntryRepository repository;
    private final KeywordRepository keywordRepository;
    private final GeneMapper mapper;
    private final AppProperties appProperties;


    // Whitelisted sortable fields from ProteinSummaryDto
    private static final Set<String> SORT_WHITELIST = Arrays.stream(ProteinSummaryDto.class.getDeclaredFields())
            .map(Field::getName).collect(Collectors.toSet());


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
        var page = request.getRequestPage(SORT_WHITELIST, "id");
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
    @Transactional(readOnly = true)
    public ProteinDetailDto getGeneById(Long id) {
        var gene = repository.findBaseDetails(id).orElseThrow(() -> ResourceNotFoundException.forProtein(id));
        gene = repository.findAdditionalDetails(id).orElseThrow(() -> ResourceNotFoundException.forProtein(id));
        return mapper.toDetail(gene);

    }

    /**
     * Streams all filtered rows as CSV into the provided writer.
     * Page configuration is ignored and all data are returned
     *
     * @see documentation/api-contract.md — POST /api/genes/export-csv
     */
    public void exportCsv(GeneSearchRequest request, Writer writer) throws IOException {
        var maxSize = appProperties.getExport().getCsv().getMaxRows();
        request.getRequestPage(SORT_WHITELIST, "id"); // to validate sort field or use default one if null
        var page = PageRequest.of(0, maxSize);
        var spec = GeneSpecification.fromRequest(request);
        var genes = repository.findAll(spec, page);
        if (genes.getTotalElements() > maxSize) {
            throw new PayloadTooLargeException("Export limit exceeded. Maximum allowed rows: " + maxSize);
        }
        var csvWriter = new CsvWriter();
        csvWriter.write(writer, genes.get().map(mapper::toSummary).toList());

    }

    public List<String> listKeywords() {
        return keywordRepository.findAll()
                .stream()
                .map(Keyword::getName)
                .toList();
    }
}
