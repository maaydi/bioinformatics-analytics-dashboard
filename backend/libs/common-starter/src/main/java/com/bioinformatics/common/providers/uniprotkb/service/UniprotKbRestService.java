package com.bioinformatics.common.providers.uniprotkb.service;

import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.common.providers.uniprotkb.dto.UniProtLightEntry;
import com.bioinformatics.common.providers.uniprotkb.gene.specification.GeneSpecification;
import com.bioinformatics.common.uniprot.dto.UniProtEntry;
import com.bioinformatics.common.uniprot.dto.UniprotKbResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Objects;

/**
 * Service for querying the UniProt KB search API.
 *
 * <p>Provides two overloaded methods for different data granularities:</p>
 * <ul>
 *   <li>{@code searchAll(GeneSearchRequest, String)} — full-fidelity protein entries with
 *       sorting and cursor-based pagination</li>
 *   <li>{@code searchAll(String, int)} — simplified search for lightweight entries</li>
 * </ul>
 *
 * <p>Both enforce page size constraints (1-500 entries) and log the effective
 * query for operational visibility.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UniprotKbRestService {

    private final RestClient uniprotRestClient;
    private final GeneSpecification geneSpecification;

    /**
     * Searches the UniProt KB using a gene search request with optional cursor-based pagination and sorting.
     *
     * @param request the search criteria and pagination state
     * @param cursor  the pagination cursor from a prior response; {@code null} for the first page
     * @return a {@link ResponseEntity} containing a paginated response of full protein entries
     */
    public ResponseEntity<UniprotKbResponse<UniProtEntry>> searchAll(GeneSearchRequest request, String cursor) {
        var queryParams = new UniprotQueryParams.Builder()
                .withPageSize(Objects.requireNonNullElse(request.size(), 500))
                .withQuery(geneSpecification.fromRequest(request))
                .withSort(request.sort(), request.direction())
                .withCursor(cursor)
                .build();
        log.debug("Search Kb API with Query : {}, Page : {}", queryParams.query(), request.page());
        return uniprotRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/uniprotkb/search")
                        .queryParams(queryParams.toQueryParams())
                        .build()
                ).retrieve()
                .toEntity(new ParameterizedTypeReference<>() {
                });
    }

    public ResponseEntity<UniprotKbResponse<UniProtLightEntry>> searchAll(String query, int pageSize) {
        var queryParams = new UniprotQueryParams.Builder()
                .withPageSize(pageSize)
                .withQuery(query)
                .build()
                .toQueryParams();
        log.debug("Search Kb API with Query : {}", query);
        return uniprotRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/uniprotkb/search")
                        .queryParams(queryParams)
                        .build()
                ).retrieve()
                .toEntity(new ParameterizedTypeReference<>() {
                });
    }
}
