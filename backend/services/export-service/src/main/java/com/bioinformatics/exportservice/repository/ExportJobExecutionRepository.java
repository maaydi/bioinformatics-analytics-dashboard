package com.bioinformatics.exportservice.repository;

import com.bioinformatics.exportservice.entity.ExportJobExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ExportJobExecution}.
 *
 * <p>Provides specialized queries for tracking batch job execution progress.
 * This denormalized table allows efficient polling of export status without
 * joining to the Spring Batch {@code BATCH_JOB_EXECUTION} table.
 */
public interface ExportJobExecutionRepository extends JpaRepository<ExportJobExecution, Long> {

    /**
     * Finds the job execution progress record for a given pipeline.
     *
     * <p>Since there is a unique constraint on {@code job_execution_id},
     * there is at most one execution per pipeline.
     *
     * @param pipelineId the ID of the export pipeline
     * @return the job execution record, or empty if not found
     */
    Optional<ExportJobExecution> findByPipelineId(@Param("pipelineId") Long pipelineId);

}

