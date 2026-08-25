package com.bioinformatics.common.gene.repository;

import com.bioinformatics.common.gene.entity.ProteinPublication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProteinPublicationRepository extends JpaRepository<ProteinPublication, Long> {

    List<ProteinPublication> findByProteinId(Long proteinId);
}

