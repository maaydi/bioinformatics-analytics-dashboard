package com.bioinformatics.common.gene.repository;

import com.bioinformatics.common.gene.entity.ProteinEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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
 * @see <a href="{@docRoot}/documentation/overview.md">Smart Query Strategy</a>
 */
public interface ProteinEntryRepository
        extends JpaRepository<ProteinEntry, Long>,
        JpaSpecificationExecutor<ProteinEntry> {

    Optional<ProteinEntry> findByAccession(String accession);

    boolean existsByAccession(String accession);

    /**
     * base detail fetch with three related collections in a single query.
     * Used exclusively for the Gene Detail page (GET /api/genes/{id}).
     */
    @Query("""
            SELECT p FROM ProteinEntry p
            LEFT JOIN FETCH p.keywords
            LEFT JOIN FETCH p.features
            LEFT JOIN FETCH p.goTerms
            WHERE p.accession = :accession
            """)
    Optional<ProteinEntry> findBaseDetails(@Param("accession") String accession);

    /**
     * Full detail fetch with the rest of  related collections in a single query.
     * Used exclusively for the Gene Detail page (GET /api/genes/{id}).
     */
    @Query("""
            SELECT p FROM ProteinEntry p
            LEFT JOIN FETCH p.hostOrganisms
            WHERE p.accession = :accession
            """)
    Optional<ProteinEntry> findAdditionalDetails(@Param("accession") String accession);

    @Query("""
                SELECT p.accession FROM ProteinEntry p
            """)
    List<String> findAllAccessions();

    @Query(value = """
            SELECT accession FROM protein_entry WHERE LOWER(accession) LIKE LOWER(CONCAT('%', :query, '%')) LIMIT 10
            """, nativeQuery = true)
    List<String> findTop10ByAccessionContainingIgnoreCase(@Param("query") String query);

    @Query(value = """
            SELECT DISTINCT entry_name FROM protein_entry WHERE LOWER(entry_name) LIKE LOWER(CONCAT('%', :query, '%')) LIMIT 10
            """, nativeQuery = true)
    List<String> findTop10ByEntryNameContainingIgnoreCase(@Param("query") String query);

    @Query(value = """
            SELECT DISTINCT gene_name_primary FROM protein_entry WHERE LOWER(gene_name_primary) LIKE LOWER(CONCAT('%', :query, '%')) LIMIT 10
            """, nativeQuery = true)
    List<String> findTop10ByGeneNamePrimaryContainingIgnoreCase(@Param("query") String query);

    @Query(value = """
            SELECT DISTINCT protein_full_name FROM protein_entry WHERE LOWER(protein_full_name) LIKE LOWER(CONCAT('%', :query, '%')) LIMIT 10
            """, nativeQuery = true)
    List<String> findTop10ByProteinFullNameContainingIgnoreCase(@Param("query") String query);

    @Query(value = """
            SELECT DISTINCT organism_name FROM protein_entry WHERE LOWER(organism_name) LIKE LOWER(CONCAT('%', :query, '%')) LIMIT 10
            """, nativeQuery = true)
    List<String> findTop10ByOrganismNameContainingIgnoreCase(@Param("query") String query);

    @Query(value = """
            SELECT DISTINCT elem FROM protein_entry, unnest(lineage) AS elem WHERE LOWER(elem) LIKE LOWER(CONCAT('%', :query, '%')) LIMIT 10
            """, nativeQuery = true)
    List<String> findTop10ByLineageContainingIgnoreCase(@Param("query") String query);


}
