package com.bioinformatics.dashboard.batch;

import com.bioinformatics.dashboard.gene.repository.ProteinEntryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class resolve the uniqueness of accession while processing uniprot
 *
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
