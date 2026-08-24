package com.bioinformatics.dashboard.providers.postgres.gene.repository;

import com.bioinformatics.common.gene.entity.ProteinFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProteinFeatureRepository extends JpaRepository<ProteinFeature, Long> {
    @Query(value = """
            SELECT DISTINCT feature_type FROM protein_feature WHERE LOWER(feature_type) LIKE LOWER(CONCAT('%', :query, '%')) LIMIT 10
            """, nativeQuery = true)
    List<String> findTop10ByFeatureTypeContainingIgnoreCase(@Param("query") String query);

}

