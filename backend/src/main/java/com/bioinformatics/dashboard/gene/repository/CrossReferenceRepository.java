package com.bioinformatics.dashboard.gene.repository;

import com.bioinformatics.dashboard.gene.entity.CrossReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrossReferenceRepository extends JpaRepository<CrossReference, Long> {

    List<CrossReference> findByProteinId(Long proteinId);
}

