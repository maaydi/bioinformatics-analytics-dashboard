package com.bioinformatics.common.gene.service;

import com.bioinformatics.common.gene.entity.ProteinEntry;
import com.bioinformatics.common.gene.repository.CrossReferenceRepository;
import com.bioinformatics.common.gene.repository.ProteinCommentRepository;
import com.bioinformatics.common.gene.repository.ProteinEntryRepository;
import com.bioinformatics.common.gene.repository.ProteinPublicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Manages operations and logic for ProteinEntryService.
 */
@Service
@RequiredArgsConstructor
public class ProteinEntryService {
    /**
     * Service that encapsulates all read access patterns for ProteinEntry and its related
     * collections (cross-references, comments, publications, features, host organisms).
     * <p>
     * Use this service for any consumer that requires a protein with full details.
     * Do NOT use the repositories directly from controllers or higher-level services
     * when loading detailed protein views — this centralizes fetch strategies,
     * prevents N+1 query problems, and provides a single place for caching/authorization
     * and future performance optimizations.
     */

    private final ProteinEntryRepository proteinEntryRepository;
    private final CrossReferenceRepository crossReferenceRepository;
    private final ProteinCommentRepository proteinCommentRepository;
    private final ProteinPublicationRepository proteinPublicationRepository;

    /**
     * Find a protein entry by its UniProt accession.
     * Returns an empty Optional when not found.
     */
    public Optional<ProteinEntry> findByAccession(String accession) {
        return proteinEntryRepository.findByAccession(accession);
    }

    /**
     * Check whether a protein with the given accession exists.
     */
    public boolean existsByAccession(String accession) {
        return proteinEntryRepository.existsByAccession(accession);
    }

    /**
     * Fetch base details for a protein (lightweight join of core fields and small collections).
     * Intended for summary/detail endpoints that do not require the full children sets.
     */
    public Optional<ProteinEntry> findBaseDetails(@Param("accession") String accession) {
        return proteinEntryRepository.findBaseDetails(accession);
    }

    /**
     * Fetch the full protein detail including large child collections.
     * This method populates transient child sets (cross-references, comments, publications)
     * by querying dedicated repositories to avoid loading huge object graphs via JPA.
     */
    public Optional<ProteinEntry> findAdditionalDetails(@Param("accession") String accession) {
        var protein = proteinEntryRepository.findAdditionalDetails(accession);
        protein.ifPresent(p -> {
            p.setCrossReferences(new HashSet<>(crossReferenceRepository.findByProteinId(p.getId())));
            p.setComments(new HashSet<>(proteinCommentRepository.findByProteinId(p.getId())));
            p.setPublications(new HashSet<>(proteinPublicationRepository.findByProteinId(p.getId())));
        });
        return protein;
    }

    /**
     * Return all accessions. Useful for bulk lookups or client-side autocomplete sources.
     */
    public List<String> findAllAccessions() {
        return proteinEntryRepository.findAllAccessions();
    }

    /**
     * Paginated fetch of protein entries (summary projection via repository mapper).
     */
    public Page<ProteinEntry> findAll(Pageable pageable) {
        return proteinEntryRepository.findAll(pageable);
    }

    /**
     * Paginated fetch with a JPA `Specification` for filtering.
     */
    public Page<ProteinEntry> findAll(Specification<ProteinEntry> spec, Pageable pageable) {
        var page = proteinEntryRepository.findAll(spec, pageable);
        var entries = page.getContent();
        if (entries.isEmpty()) {
            return page;
        }
        var proteinIds = entries.stream().map(ProteinEntry::getId).toList();
        var crossRefsMap = crossReferenceRepository.findByProtein_IdIn(proteinIds)
                .stream().collect(Collectors.groupingBy(c -> c.getProtein().getId()));

        var commentsMap = proteinCommentRepository.findByProtein_IdIn(proteinIds)
                .stream().collect(Collectors.groupingBy(c -> c.getProtein().getId()));

        var publicationsMap = proteinPublicationRepository.findByProtein_IdIn(proteinIds)
                .stream().collect(Collectors.groupingBy(p -> p.getProtein().getId()));

        for (var entry : entries) {
            entry.setCrossReferences(new HashSet<>(crossRefsMap.getOrDefault(entry.getId(), List.of())));
            entry.setComments(new HashSet<>(commentsMap.getOrDefault(entry.getId(), List.of())));
            entry.setPublications(new HashSet<>(publicationsMap.getOrDefault(entry.getId(), List.of())));
        }

        return page;
    }

    public long count(Specification<ProteinEntry> spec) {
        return proteinEntryRepository.count(spec);
    }
}
