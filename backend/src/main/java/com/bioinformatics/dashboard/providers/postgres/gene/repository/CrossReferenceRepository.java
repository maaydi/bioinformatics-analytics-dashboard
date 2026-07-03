package com.bioinformatics.dashboard.providers.postgres.gene.repository;

import com.bioinformatics.dashboard.providers.postgres.gene.entity.CrossReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrossReferenceRepository extends JpaRepository<CrossReference, Long> {

    List<CrossReference> findByProteinId(Long proteinId);
}

