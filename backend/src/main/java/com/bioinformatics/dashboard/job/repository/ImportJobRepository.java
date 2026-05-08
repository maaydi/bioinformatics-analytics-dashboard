package com.bioinformatics.dashboard.job.repository;

import com.bioinformatics.dashboard.job.dto.ImportStatus;
import com.bioinformatics.dashboard.job.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface ImportJobRepository extends JpaRepository<ImportJob, UUID>,
        JpaSpecificationExecutor<ImportJob> {


    List<ImportJob> findByStatus(ImportStatus status);


}
