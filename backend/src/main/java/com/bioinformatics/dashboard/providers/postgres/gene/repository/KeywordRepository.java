package com.bioinformatics.dashboard.providers.postgres.gene.repository;

import com.bioinformatics.dashboard.providers.postgres.gene.entity.Keyword;
import com.bioinformatics.dashboard.providers.postgres.gene.entity.ProteinEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Spring Data JPA repository for {@link Keyword}.
 */
public interface KeywordRepository
        extends JpaRepository<Keyword, Integer>,
        JpaSpecificationExecutor<ProteinEntry> {
    Optional<Keyword> findByName(String name);

    @Query("""
            SELECT k FROM Keyword k WHERE k.name IN :names
            """)
    List<Keyword> findAllByNamesIn(Set<String> names);
}
