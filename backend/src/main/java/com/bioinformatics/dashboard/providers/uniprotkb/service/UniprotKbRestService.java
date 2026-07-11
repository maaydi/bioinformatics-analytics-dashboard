package com.bioinformatics.dashboard.providers.uniprotkb.service;

import com.bioinformatics.dashboard.providers.uniprotkb.dto.UniProtEntry;
import com.bioinformatics.dashboard.providers.uniprotkb.dto.UniprotKbResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class UniprotKbRestService {

    private final RestClient uniprotRestClient;

    /**
     * Used to first page search
     *
     */
    public ResponseEntity<UniprotKbResponse<UniProtEntry>> search(int pageSize) {
        assert pageSize > 0 : "Page size must be greater than zero";
        assert pageSize <= 500 : "Page size must be less than or equal to 500";
        var queryValue = "(*)";
        return uniprotRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/search")
                        .queryParam("format", "json")
                        .queryParam("query", queryValue)
                        .queryParam("size", pageSize)
                        .build()
                ).retrieve()
                .toEntity(new ParameterizedTypeReference<UniprotKbResponse<UniProtEntry>>() {
                });
    }

    public ResponseEntity<UniprotKbResponse<UniProtEntry>> searchAll(int pageSize, String cursor) {
        assert pageSize > 0 : "Page size must be greater than zero";
        assert pageSize <= 500 : "Page size must be less than or equal to 500";
        var queryValue = "(*)";
        return uniprotRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/search")
                        .queryParam("format", "json")
                        .queryParam("query", queryValue)
                        .queryParam("size", pageSize)
                        .queryParam("cursor", cursor)
                        .build()
                ).retrieve()
                .toEntity(new ParameterizedTypeReference<UniprotKbResponse<UniProtEntry>>() {
                });
    }
}
