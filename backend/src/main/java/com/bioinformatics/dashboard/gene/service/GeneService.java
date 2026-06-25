package com.bioinformatics.dashboard.gene.service;

import com.bioinformatics.dashboard.config.AppProperties;
import com.bioinformatics.dashboard.csv.CsvWriter;
import com.bioinformatics.dashboard.exception.ExportRowCapExceededException;
import com.bioinformatics.dashboard.exception.ResourceNotFoundException;
import com.bioinformatics.dashboard.gene.dto.GeneSearchRequest;
import com.bioinformatics.dashboard.gene.dto.PagedResponse;
import com.bioinformatics.dashboard.gene.dto.ProteinDetailDto;
import com.bioinformatics.dashboard.gene.dto.ProteinSummaryDto;
import com.bioinformatics.dashboard.gene.entity.Keyword;
import com.bioinformatics.dashboard.gene.mapper.GeneMapper;
import com.bioinformatics.dashboard.gene.repository.KeywordRepository;
import com.bioinformatics.dashboard.gene.specification.GeneSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
@Slf4j
public class GeneService {

    private final ProteinEntryService proteinService;
    private final KeywordRepository keywordRepository;
    private final GeneMapper mapper;
    private final AppProperties appProperties;


    // Whitelisted sortable fields from ProteinSummaryDto
    private static final Set<String> SORT_WHITELIST = Arrays.stream(ProteinSummaryDto.class.getDeclaredFields())
            .map(Field::getName).collect(Collectors.toSet());


    /**
     * Returns a paginated, optionally sorted list of all proteins.
     *
     */
    @Cacheable(value = "geneList", key = "#pageNumber + '-' + #size + '-' + #sort + '-' + #direction")
    public PagedResponse<ProteinSummaryDto> listGenes(int pageNumber, int size, String sort, String direction) {
        var direct = Sort.Direction.fromString(direction);
        var pageable = PageRequest.of(pageNumber, size, direct, sort);
        log.info("Retrieving all protein entries for page: {}", pageable.getPageNumber());
        var page = proteinService.findAll(pageable);
        var genes = page.getContent().stream().map(mapper::toSummary).toList();
        return new PagedResponse<>(genes, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    /**
     * Returns a paginated filtered result set.
     *
     */
    @Cacheable(value = "geneSearch", key = "#request.toString()")
    public PagedResponse<ProteinSummaryDto> searchGenes(GeneSearchRequest request) {
        log.info("Searching for protein entries for filters: {}", request);
        var page = request.getRequestPage(SORT_WHITELIST, "id");
        var spec = GeneSpecification.fromRequest(request);
        var result = proteinService.findAll(spec, page);
        var genes = result.getContent().stream().map(mapper::toSummary).toList();
        return new PagedResponse<>(genes, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());

    }

    /**
     * Returns the full detail of a single protein entry.
     *
     * @throws ResourceNotFoundException if not found
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "geneDetail", key = "#id")
    public ProteinDetailDto getGeneById(Long id) {
        log.info("Retrieving protein entry by id: {}", id);
        var gene = proteinService.findAdditionalDetails(id).orElseThrow(() -> ResourceNotFoundException.forProtein(id));
        return mapper.toDetail(gene);

    }

    /**
     * Streams all filtered rows as CSV into the provided writer.
     * Page configuration is ignored and all data are returned
     *
     */
    public void exportCsv(GeneSearchRequest request, Writer writer, long totalRows) throws IOException {
        log.info("Exporting protein entries for filters: {}", request);
        request.getRequestPage(SORT_WHITELIST, "id");
        var page = PageRequest.of(0, (int) totalRows);
        var spec = GeneSpecification.fromRequest(request);
        var genes = proteinService.findAll(spec, page);
        var csvWriter = new CsvWriter();
        csvWriter.write(writer, genes.get().map(mapper::toSummary).toList());

    }

    public long assertWithinExportLimit(GeneSearchRequest request) {
        var maxSize = appProperties.getExport().getCsv().getMaxRows();
        var spec = GeneSpecification.fromRequest(request);
        var totalRows = proteinService.count(spec);
        if (totalRows > maxSize) {
            throw new ExportRowCapExceededException("Export limit exceeded. Result contains %d rows; maximum is %d. Please refine your filter"
                    .formatted(totalRows, maxSize));
        }
        return totalRows;

    }

    @Cacheable(value = "geneKeywords", cacheManager = "listStringCacheManager")
    public List<String> listKeywords() {
        log.info("Retrieving keywords for protein entries");
        return keywordRepository.findAll()
                .stream()
                .map(Keyword::getName)
                .toList();
    }
}
