package com.bioinformatics.dashboard.job.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.bioinformatics.dashboard.job.entity.ImportJob;

public interface ImportJobRepository extends JpaRepository<ImportJob, UUID>,
        JpaSpecificationExecutor<ImportJob> {

}
