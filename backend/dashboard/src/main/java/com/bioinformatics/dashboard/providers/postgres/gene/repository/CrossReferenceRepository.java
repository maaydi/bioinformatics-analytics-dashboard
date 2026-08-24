package com.bioinformatics.dashboard.providers.postgres.gene.repository;

import com.bioinformatics.common.gene.entity.CrossReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CrossReferenceRepository extends JpaRepository<CrossReference, Long> {

    List<CrossReference> findByProteinId(Long proteinId);

    @Query(value = """
            SELECT DISTINCT source FROM cross_reference WHERE LOWER(source) LIKE LOWER(CONCAT('%', :query, '%')) LIMIT 10
            """, nativeQuery = true)
    List<String> findTop10BySourceContainingIgnoreCase(@Param("query") String query);


}

