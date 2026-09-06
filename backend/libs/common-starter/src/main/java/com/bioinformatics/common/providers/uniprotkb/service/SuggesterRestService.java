package com.bioinformatics.common.providers.uniprotkb.service;

import com.bioinformatics.common.providers.uniprotkb.dto.SuggestionResult;
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

/**
 * Supported field [keyword, subcell, main, taxonomy, go, ec, catalytic_activity, cofactor, binding, organism, host,
 * chebi, proteome_upid, uniparc_taxonomy, uniparc_organism, proteome_taxonomy, proteome_organism, disease].
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SuggesterRestService {

    private final RestClient uniprotRestClient;

    @Retryable(retryFor = {ResourceAccessException.class, HttpServerErrorException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 2000, multiplier = 2))
    public ResponseEntity<SuggestionResult> searchAll(String field, String query) {
        log.info("Search Suggestion for {} with Query : {}", field, query);
        var queryParams = new UniprotQueryParams.Builder()
                .withDict(field)
                .withQuery(query)
                .build()
                .toQueryParams();
        return uniprotRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/suggester")
                        .queryParams(queryParams)
                        .build()
                ).retrieve()
                .toEntity(new ParameterizedTypeReference<>() {
                });
    }
}
