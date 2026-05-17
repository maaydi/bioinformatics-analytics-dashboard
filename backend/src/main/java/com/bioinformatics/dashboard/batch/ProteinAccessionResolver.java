package com.bioinformatics.dashboard.batch;

import com.bioinformatics.dashboard.gene.repository.ProteinEntryRepository;
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
    private final ProteinEntryRepository proteinEntryRepository;
    private final Set<String> accessions = ConcurrentHashMap.newKeySet();


    @PostConstruct
    public void init() {
        accessions.addAll(proteinEntryRepository.findAllAccessions());
    }

    public boolean alreadyExists(String accession) {
        return !accessions.add(accession);
    }
}
