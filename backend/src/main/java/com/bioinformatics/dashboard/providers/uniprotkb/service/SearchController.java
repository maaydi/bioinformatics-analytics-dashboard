package com.bioinformatics.dashboard.providers.uniprotkb.service;

import com.bioinformatics.dashboard.providers.uniprotkb.dto.UniProtEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final UniprotKbRestService service;

    @GetMapping("/uniprot")
    public ResponseEntity<List<UniProtEntry>> search(@RequestParam("reviewed") boolean reviewed) {
        var job = service.searchByReviewStatus(reviewed);
        return ResponseEntity.accepted().body(job.results());
    }
}
