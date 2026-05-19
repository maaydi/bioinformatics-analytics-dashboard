package com.bioinformatics.dashboard.gene.repository;

import com.bioinformatics.dashboard.gene.entity.ProteinComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProteinCommentRepository extends JpaRepository<ProteinComment, Long> {
}

