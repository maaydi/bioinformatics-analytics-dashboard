package com.bioinformatics.dashboard.gene.service;

import com.bioinformatics.dashboard.gene.entity.ProteinEntry;
import com.bioinformatics.dashboard.gene.repository.CrossReferenceRepository;
import com.bioinformatics.dashboard.gene.repository.ProteinCommentRepository;
import com.bioinformatics.dashboard.gene.repository.ProteinEntryRepository;
import com.bioinformatics.dashboard.gene.repository.ProteinPublicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProteinEntryService {

    private final ProteinEntryRepository proteinEntryRepository;
    private final CrossReferenceRepository crossReferenceRepository;
    private final ProteinCommentRepository proteinCommentRepository;
    private final ProteinPublicationRepository proteinPublicationRepository;


    public Optional<ProteinEntry> findByAccession(String accession) {
        return proteinEntryRepository.findByAccession(accession);
    }

    public boolean existsByAccession(String accession) {
        return proteinEntryRepository.existsByAccession(accession);
    }

    /**
     * base detail fetch with three related collections in a single query.
     * Used exclusively for the Gene Detail page (GET /api/genes/{id}).
     */
    public Optional<ProteinEntry> findBaseDetails(@Param("id") Long id) {
        return proteinEntryRepository.findBaseDetails(id);
    }

    /**
     * Full detail fetch with the rest of  related collections in a single query.
     * Used exclusively for the Gene Detail page (GET /api/genes/{id}).
     */
    public Optional<ProteinEntry> findAdditionalDetails(@Param("id") Long id) {
        var protein = proteinEntryRepository.findAdditionalDetails(id);
        protein.ifPresent(p -> {
            p.setCrossReferences(new HashSet<>(crossReferenceRepository.findByProteinId(p.getId())));
            p.setComments(new HashSet<>(proteinCommentRepository.findByProteinId(p.getId())));
            p.setPublications(new HashSet<>(proteinPublicationRepository.findByProteinId(p.getId())));
        });
        return protein;
    }


    public List<String> findAllAccessions() {
        return proteinEntryRepository.findAllAccessions();
    }

    public Page<ProteinEntry> findAll(Pageable pageable) {
        return proteinEntryRepository.findAll(pageable);
    }

    public Page<ProteinEntry> findAll(Specification<ProteinEntry> spec, Pageable pageable) {
        return proteinEntryRepository.findAll(spec, pageable);
    }
}
