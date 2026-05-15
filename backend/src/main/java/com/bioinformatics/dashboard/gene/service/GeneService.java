package com.bioinformatics.dashboard.gene.service;

import com.bioinformatics.dashboard.csv.CsvWriter;
import com.bioinformatics.dashboard.exception.ResourceNotFoundException;
import com.bioinformatics.dashboard.gene.dto.GeneSearchRequest;
import com.bioinformatics.dashboard.gene.dto.PagedResponse;
import com.bioinformatics.dashboard.gene.dto.ProteinDetailDto;
import com.bioinformatics.dashboard.gene.dto.ProteinSummaryDto;
import com.bioinformatics.dashboard.gene.mapper.GeneMapper;
import com.bioinformatics.dashboard.gene.repository.ProteinEntryRepository;
import com.bioinformatics.dashboard.gene.specification.GeneSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
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
    private final GeneMapper mapper;
    private final CsvWriter csvWriter;


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
        var dir = request.direction() == null ? "asc" : request.direction();
        var direct = Sort.Direction.fromString(dir);

        var sortField = request.sort() == null ? "id" : request.sort();
        if (!SORT_WHITELIST.contains(sortField)) {
            throw new IllegalArgumentException("Invalid sort field: '" + sortField + "'. Allowed fields: " + SORT_WHITELIST);
        }

        var page = PageRequest.of(request.page(), request.size(), direct, sortField);
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
    public ProteinDetailDto getGeneById(Long id) {
        var gene = repository.findByIdWithAllRelations(id).orElseThrow(() -> ResourceNotFoundException.forProtein(id));
        return mapper.toDetail(gene);

    }

    /**
     * Streams all filtered rows as CSV into the provided writer.
     * Page configuration is ignored and all data are returned
     *
     * @see documentation/api-contract.md — POST /api/genes/export-csv
     */
    public void exportCsv(GeneSearchRequest request, java.io.Writer writer) throws IOException {
        var spec = GeneSpecification.fromRequest(request);
        var genes = repository.findAll(spec).stream().map(mapper::toSummary).toList();
        csvWriter.write(writer, genes);

    }
}
