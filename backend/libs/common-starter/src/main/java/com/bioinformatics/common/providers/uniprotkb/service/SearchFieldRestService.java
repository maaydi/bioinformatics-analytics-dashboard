package com.bioinformatics.common.providers.uniprotkb.service;

import com.bioinformatics.common.providers.uniprotkb.dto.searchfield.SearchField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Service for retrieving UniProt KB search field configuration.
 *
 * <p>Queries the UniProt API's configuration endpoint to fetch metadata about
 * available search fields, their data types, allowed values, and hierarchical structure.
 * This configuration is typically loaded once during application startup and
 * cached for the lifetime of the application.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchFieldRestService {

    private final RestClient uniprotRestClient;

    /**
     * Loads the complete search field configuration from the UniProt API.
     *
     * <p>The configuration includes field hierarchies, data type constraints, allowed values,
     * and evidence groupings that enable faceted search and query validation.</p>
     *
     * @return a {@link ResponseEntity} containing a list of top-level search field definitions
     */
    @Retryable(retryFor = {ResourceAccessException.class, HttpServerErrorException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 2000, multiplier = 2))
    public ResponseEntity<List<SearchField>> loadSearchFieldConfig() {
        log.info("Load search field config ");
        return uniprotRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/configure/uniprotkb/search-fields")
                        .build()
                ).retrieve()
                .toEntity(new ParameterizedTypeReference<List<SearchField>>() {
                });
    }
}
