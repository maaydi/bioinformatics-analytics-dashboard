package com.bioinformatics.dashboard.providers.postgres.gene.repository;

import com.bioinformatics.common.gene.entity.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Keyword}.
 */
public interface KeywordRepository
        extends JpaRepository<Keyword, Integer> {

    @Query(value = """
            SELECT DISTINCT name FROM keyword WHERE LOWER(name) LIKE LOWER(CONCAT('%', :query, '%')) LIMIT 10
            """, nativeQuery = true)
    List<String> findTop10ByNameContainingIgnoreCase(@Param("query") String query);
}
