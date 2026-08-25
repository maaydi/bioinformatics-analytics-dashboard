package com.bioinformatics.importservice.repository;

import com.bioinformatics.importservice.dto.ImportStatus;
import com.bioinformatics.importservice.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ImportJobRepository extends JpaRepository<ImportJob, UUID>,
        JpaSpecificationExecutor<ImportJob> {


    List<ImportJob> findByStatus(ImportStatus status);

    @Modifying
    @Query("""
                update ImportJob i
                set i.status = :newStatus
                where i.status = :currentStatus
            """)
    void updateStatusInBulk(
            ImportStatus currentStatus,
            ImportStatus newStatus);


}
