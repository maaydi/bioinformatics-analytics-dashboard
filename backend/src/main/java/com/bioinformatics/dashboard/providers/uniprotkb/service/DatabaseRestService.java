package com.bioinformatics.dashboard.providers.uniprotkb.service;

import com.bioinformatics.dashboard.model.uniprot.dto.UniprotKbResponse;
import com.bioinformatics.dashboard.providers.uniprotkb.dto.CrossRefLightEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseRestService {

    private final RestClient uniprotRestClient;

    public ResponseEntity<UniprotKbResponse<CrossRefLightEntry>> searchAll(String query, int pageSize) {
        var queryParams = new LinkedMultiValueMap<String, String>();
        queryParams.add("format", "json");
        assert pageSize > 0 : "Page size must be greater than zero";
        assert pageSize <= 500 : "Page size must be less than or equal to 500";
        queryParams.add("size", String.valueOf(pageSize));
        queryParams.add("query", query);
        log.info("Search Cross reference API with Query : {}", query);
        return uniprotRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/database/search")
                        .queryParams(queryParams)
                        .build()
                ).retrieve()
                .toEntity(new ParameterizedTypeReference<UniprotKbResponse<CrossRefLightEntry>>() {
                });
    }
}
