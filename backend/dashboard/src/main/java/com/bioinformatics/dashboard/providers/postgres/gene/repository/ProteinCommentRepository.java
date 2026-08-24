package com.bioinformatics.dashboard.providers.postgres.gene.repository;

import com.bioinformatics.common.gene.entity.ProteinComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProteinCommentRepository extends JpaRepository<ProteinComment, Long> {

    List<ProteinComment> findByProteinId(Long proteinId);
}

