package com.bioinformatics.dashboard.providers.uniprotkb.service;

import com.bioinformatics.dashboard.model.gene.GeneSearchRequest;
import com.bioinformatics.dashboard.model.uniprot.dto.UniProtEntry;
import com.bioinformatics.dashboard.model.uniprot.dto.UniprotKbResponse;
import com.bioinformatics.dashboard.providers.uniprotkb.dto.UniProtLightEntry;
import com.bioinformatics.dashboard.providers.uniprotkb.gene.specification.GeneSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class UniprotKbRestService {

    private final RestClient uniprotRestClient;

    public ResponseEntity<UniprotKbResponse<UniProtEntry>> searchAll(GeneSearchRequest request, String cursor) {
        var queryParams = new LinkedMultiValueMap<String, String>();
        queryParams.add("format", "json");
        var pageSize = Objects.requireNonNullElse(request.size(), 500);
        assert pageSize > 0 : "Page size must be greater than zero";
        assert pageSize <= 500 : "Page size must be less than or equal to 500";
        queryParams.add("size", String.valueOf(pageSize));
        var queryValue = GeneSpecification.fromRequest(request);
        queryParams.add("query", queryValue);
        if (request.sort() != null && request.direction() != null) {
            queryParams.add("sort", "%s %s".formatted(request.sort(), request.direction()));
        }
        if (cursor != null) {
            queryParams.add("cursor", cursor);
        }
        log.info("Search Kb API with Query : {}, Page : {}", queryValue, request.page());
        return uniprotRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/uniprotkb/search")
                        .queryParams(queryParams)
                        .build()
                ).retrieve()
                .toEntity(new ParameterizedTypeReference<UniprotKbResponse<UniProtEntry>>() {
                });
    }

    public ResponseEntity<UniprotKbResponse<UniProtLightEntry>> searchAll(String query, int pageSize) {
        var queryParams = new LinkedMultiValueMap<String, String>();
        queryParams.add("format", "json");
        assert pageSize > 0 : "Page size must be greater than zero";
        assert pageSize <= 500 : "Page size must be less than or equal to 500";
        queryParams.add("size", String.valueOf(pageSize));
        queryParams.add("query", "query");
        log.info("Search Kb API with Query : {}", query);
        return uniprotRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/uniprotkb/search")
                        .queryParams(queryParams)
                        .build()
                ).retrieve()
                .toEntity(new ParameterizedTypeReference<UniprotKbResponse<UniProtLightEntry>>() {
                });
    }
}
