package com.bioinformatics.common.providers.uniprotkb.service;

import com.bioinformatics.common.providers.uniprotkb.dto.CrossRefLightEntry;
import com.bioinformatics.common.uniprot.dto.UniprotKbResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Service for querying UniProt database cross-references via the database search API.
 *
 * <p>Delegates to the UniProt REST API to retrieve lightweight cross-reference entries,
 * useful for identifying related databases and performing cross-referential lookups.
 * Enforces page size constraints (1-500 entries) to prevent excessive memory usage.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseRestService {

    private final RestClient uniprotRestClient;

    /**
     * Searches for database cross-reference entries matching the provided query.
     *
     * @param query    the search query string (e.g., database ID or name pattern)
     * @param pageSize the desired number of results per page; must be between 1 and 500
     * @return a {@link ResponseEntity} containing a paginated response of cross-reference entries
     * @throws IllegalArgumentException if {@code pageSize} is outside the valid range
     */
    public ResponseEntity<UniprotKbResponse<CrossRefLightEntry>> searchAll(String query, int pageSize) {
        var queryParams = new UniprotQueryParams.Builder()
                .withPageSize(pageSize)
                .withQuery(query)
                .build()
                .toQueryParams();
        log.info("Search Cross reference API with Query : {}", query);
        return uniprotRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/database/search")
                        .queryParams(queryParams)
                        .build()
                ).retrieve()
                .toEntity(new ParameterizedTypeReference<>() {
                });
    }
}
