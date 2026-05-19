package com.bioinformatics.dashboard.gene.repository;

import com.bioinformatics.dashboard.gene.entity.CrossReference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrossReferenceRepository extends JpaRepository<CrossReference, Long> {
}

