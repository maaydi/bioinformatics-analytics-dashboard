package com.bioinformatics.dashboard.providers.postgres.gene.repository;

import com.bioinformatics.dashboard.providers.postgres.gene.entity.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Spring Data JPA repository for {@link Keyword}.
 */
public interface KeywordRepository
        extends JpaRepository<Keyword, Integer> {
    Optional<Keyword> findByName(String name);

    @Query("""
            SELECT k FROM Keyword k WHERE k.name IN :names
            """)
    List<Keyword> findAllByNamesIn(Set<String> names);

    @Query(value = """
            SELECT DISTINCT name FROM keyword WHERE LOWER(name) LIKE LOWER(CONCAT('%', :query, '%')) LIMIT 10
            """, nativeQuery = true)
    List<String> findTop10ByNameContainingIgnoreCase(@Param("query") String query);
}
