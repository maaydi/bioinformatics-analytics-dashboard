package com.bioinformatics.dashboard.job.resolver;

import com.bioinformatics.dashboard.providers.postgres.gene.service.ProteinEntryService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains an in-memory set of existing UniProt accessions and provides
 * a fast check to avoid inserting duplicate protein entries during import.
 */
@Component
@RequiredArgsConstructor
public class ProteinAccessionResolver {
    private final ProteinEntryService proteinEntryService;
    private final Set<String> accessions = ConcurrentHashMap.newKeySet();


    @PostConstruct
    public void init() {
        accessions.addAll(proteinEntryService.findAllAccessions());
    }

    public boolean alreadyExists(String accession) {
        return !accessions.add(accession);
    }
}
