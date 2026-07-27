package com.bioinformatics.dashboard.providers.postgres.gene.repository;

import com.bioinformatics.dashboard.providers.postgres.gene.entity.GoTerm;
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
public interface GoTermRepository
        extends JpaRepository<GoTerm, Integer> {
    Optional<GoTerm> findByGoId(String goId);

    @Query("""
            SELECT go FROM GoTerm go WHERE go.goId IN :goIds
            """)
    List<GoTerm> findAllByGoIdIn(Set<String> goIds);

    @Query(value = """
            SELECT go_id FROM go_term WHERE LOWER(go_id) LIKE LOWER(CONCAT('%', :query, '%')) LIMIT 10
            """, nativeQuery = true)
    List<String> findTop10ByGoIdContainingIgnoreCase(@Param("query") String query);
}
