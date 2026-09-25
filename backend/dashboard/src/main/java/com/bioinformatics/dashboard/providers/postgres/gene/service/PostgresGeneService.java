package com.bioinformatics.dashboard.providers.postgres.gene.service;

import com.bioinformatics.common.exception.ResourceNotFoundException;
import com.bioinformatics.common.gene.dto.ProteinDetailDto;
import com.bioinformatics.common.gene.dto.ProteinSummaryDto;
import com.bioinformatics.common.gene.mapper.GeneMapper;
import com.bioinformatics.common.gene.service.ProteinEntryService;
import com.bioinformatics.common.gene.specification.GeneSpecification;
import com.bioinformatics.common.models.PagedResponse;
import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.common.providers.postgres.AbstractPostgresProvider;
import com.bioinformatics.dashboard.config.AppProperties;
import com.bioinformatics.dashboard.interfaces.gene.GeneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for gene/protein operations.
 *
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PostgresGeneService extends AbstractPostgresProvider implements GeneService {

    private final ProteinEntryService proteinService;
    private final GeneMapper mapper;
    private final AppProperties appProperties;

    /**
     * Returns a paginated, optionally sorted list of all proteins.
     *
     */
    @Override
    @Cacheable(value = "geneList", key = "#pageNumber + '-' + #size + '-' + #sort + '-' + #direction")
    @Transactional(readOnly = true)
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
    @Override
    @Cacheable(value = "geneSearch", key = "#request.toString()")
    @Transactional(readOnly = true)
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
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "geneDetail", key = "#accession", cacheManager = "redisNonFinalAndRecordCacheManager")
    public ProteinDetailDto getGeneByAccession(String accession) {
        log.info("Retrieving protein entry by id: {}", accession);
        var gene = proteinService.findAdditionalDetails(accession).orElseThrow(() -> ResourceNotFoundException.forProtein(accession));
        return mapper.toDetail(gene);

    }

    @Override
    public long count(GeneSearchRequest request) {
        var spec = GeneSpecification.fromRequest(request);
        return proteinService.count(spec);

    }
}
