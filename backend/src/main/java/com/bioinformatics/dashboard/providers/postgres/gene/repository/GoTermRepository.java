package com.bioinformatics.dashboard.providers.postgres.gene.repository;

import com.bioinformatics.dashboard.providers.postgres.gene.entity.GoTerm;
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
public interface GoTermRepository
        extends JpaRepository<GoTerm, Integer>,
        JpaSpecificationExecutor<ProteinEntry> {
    Optional<GoTerm> findByGoId(String goId);

    @Query("""
            SELECT go FROM GoTerm go WHERE go.goId IN :goIds
            """)
    List<Keyword> findAllByGoIdIn(Set<String> goIds);
}
