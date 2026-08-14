package com.bioinformatics.dashboard.providers.postgres.gene.repository;

import com.bioinformatics.dashboard.providers.postgres.gene.entity.ProteinPublication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProteinPublicationRepository extends JpaRepository<ProteinPublication, Long> {

    List<ProteinPublication> findByProteinId(Long proteinId);
}

