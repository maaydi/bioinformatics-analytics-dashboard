package com.bioinformatics.dashboard.gene.repository;

import com.bioinformatics.dashboard.gene.entity.Keyword;
import com.bioinformatics.dashboard.gene.entity.ProteinEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Keyword}.
 */
public interface KeywordRepository
        extends JpaRepository<Keyword, Integer>,
        JpaSpecificationExecutor<ProteinEntry> {
    Optional<Keyword> findByName(String name);
}
