package com.bioinformatics.dashboard.gene.repository;

import com.bioinformatics.dashboard.gene.entity.ProteinEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ProteinEntry}.
 *
 * <p>Implements {@link JpaSpecificationExecutor} to support dynamic multi-filter
 * queries via JPA Specifications (see {@code GeneSpecification}).
 *
 * <p>All queries must avoid N+1.  Use {@code @EntityGraph} or JOIN FETCH for
 * detail-page queries that need to load related collections.
 *
 * @see documentation/overview.md §9 — Smart Query Strategy
 */
public interface ProteinEntryRepository
        extends JpaRepository<ProteinEntry, Long>,
                JpaSpecificationExecutor<ProteinEntry> {

    Optional<ProteinEntry> findByAccession(String accession);

    boolean existsByAccession(String accession);

    /**
     * Full detail fetch with all related collections in a single query.
     * Used exclusively for the Gene Detail page (GET /api/genes/{id}).
     */
    @Query("""
            SELECT p FROM ProteinEntry p
            LEFT JOIN FETCH p.keywords
            LEFT JOIN FETCH p.features
            LEFT JOIN FETCH p.goTerms
            LEFT JOIN FETCH p.crossReferences
            LEFT JOIN FETCH p.hostOrganisms
            WHERE p.id = :id
            """)
    Optional<ProteinEntry> findByIdWithAllRelations(@Param("id") Long id);
}
