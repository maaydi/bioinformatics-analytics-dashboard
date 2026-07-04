package com.bioinformatics.dashboard.providers.uniprotkb.service;

import com.bioinformatics.dashboard.providers.uniprotkb.dto.UniProtEntry;
import com.bioinformatics.dashboard.providers.uniprotkb.dto.UniprotKbResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class UniprotKbRestService {

    private final RestClient uniprotRestClient;


    public UniprotKbResponse<UniProtEntry> searchByReviewStatus(boolean reviewed) {
        var queryValue = "((reviewed:" + reviewed + "))";
        return uniprotRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/search")
                        .queryParam("format", "json")
                        .queryParam("query", queryValue)
                        .queryParam("size", 50)
                        .build()
                ).retrieve()
                .body(new ParameterizedTypeReference<UniprotKbResponse<UniProtEntry>>() {
                });
    }
}
