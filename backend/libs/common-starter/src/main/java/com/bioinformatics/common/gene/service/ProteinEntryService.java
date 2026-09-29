package com.bioinformatics.common.gene.service;

import com.bioinformatics.common.gene.entity.ProteinEntry;
import com.bioinformatics.common.gene.repository.CrossReferenceRepository;
import com.bioinformatics.common.gene.repository.ProteinCommentRepository;
import com.bioinformatics.common.gene.repository.ProteinEntryRepository;
import com.bioinformatics.common.gene.repository.ProteinPublicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Central service for read access to protein entries and their related domain collections.
 *
 * <p>This class acts as the boundary between the JPA repositories and the rest of the application.
 * It keeps fetch strategies centralized, reduces N+1 risk by batching related collection lookups, and
 * exposes the common access patterns used by list, detail, and batch import workflows.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProteinEntryService {
    /**
     * Service that encapsulates all read access patterns for ProteinEntry and its related
     * collections (cross-references, comments, publications, features, host organisms).
     *
     * <p>Use this service for any consumer that requires a protein with full details.
     * This centralizes fetch strategies and prevents N+1 query problems in detail-heavy screen loads.
     */

    private final ProteinEntryRepository proteinEntryRepository;
    private final CrossReferenceRepository crossReferenceRepository;
    private final ProteinCommentRepository proteinCommentRepository;
    private final ProteinPublicationRepository proteinPublicationRepository;

    /**
     * Find a protein entry by its UniProt accession.
     *
     * @param accession accession to look up
     * @return protein when present
     */
    public Optional<ProteinEntry> findByAccession(String accession) {
        log.debug("Looking up ProteinEntry by accession={}", accession);
        return proteinEntryRepository.findByAccession(accession);
    }

    /**
     * Check whether a protein with the given accession exists.
     */
    public boolean existsByAccession(String accession) {
        return proteinEntryRepository.existsByAccession(accession);
    }

    /**
     * Fetch the base protein data needed for summary and lightweight detail views.
     */
    public Optional<ProteinEntry> findBaseDetails(@Param("accession") String accession) {
        log.debug("Loading base protein details for accession={}", accession);
        return proteinEntryRepository.findBaseDetails(accession);
    }

    /**
     * Fetch the full protein detail including large child collections.
     */
    public Optional<ProteinEntry> findAdditionalDetails(@Param("accession") String accession) {
        log.debug("Loading additional details for accession={}", accession);
        var protein = proteinEntryRepository.findAdditionalDetails(accession);
        protein.ifPresent(p -> {
            var crossRefs = crossReferenceRepository.findByProteinId(p.getId());
            var comments = proteinCommentRepository.findByProteinId(p.getId());
            var publications = proteinPublicationRepository.findByProteinId(p.getId());
            p.setCrossReferences(new HashSet<>(crossRefs));
            p.setComments(new HashSet<>(comments));
            p.setPublications(new HashSet<>(publications));
            log.debug("Loaded additional details for accession={} with crossRefs={}, comments={}, publications={}",
                    accession, crossRefs.size(), comments.size(), publications.size());
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
     * Paginated fetch of protein entries.
     */
    public Page<ProteinEntry> findAll(Pageable pageable) {
        log.debug("Fetching paged ProteinEntry list with pageNumber={} pageSize={}", pageable.getPageNumber(), pageable.getPageSize());
        return proteinEntryRepository.findAll(pageable);
    }

    /**
     * Paginated fetch with a JPA specification for dynamic filtering.
     */
    public Page<ProteinEntry> findAll(Specification<ProteinEntry> spec, Pageable pageable) {
        log.debug("Fetching filtered ProteinEntry page with pageNumber={} pageSize={}", pageable.getPageNumber(), pageable.getPageSize());
        var page = proteinEntryRepository.findAll(spec, pageable);
        var entries = page.getContent();
        if (entries.isEmpty()) {
            log.debug("Filtered ProteinEntry query returned no rows");
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

        log.debug("Loaded filtered page with {} entries and related data for {} proteins", entries.size(), proteinIds.size());
        return page;
    }

    public long count(Specification<ProteinEntry> spec) {
        return proteinEntryRepository.count(spec);
    }
}
