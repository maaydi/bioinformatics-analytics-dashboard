package com.bioinformatics.dashboard.providers.uniprotkb.service;

import com.bioinformatics.dashboard.providers.uniprotkb.dto.searchfield.SearchField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchFieldRestService {

    private final RestClient uniprotRestClient;

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
