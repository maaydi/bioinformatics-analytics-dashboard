package com.bioinformatics.exportservice.repository;

import com.bioinformatics.exportservice.dto.ExportStatus;
import com.bioinformatics.exportservice.entity.ExportPipeline;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ExportPipeline}.
 *
 * <p>Provides specialized query methods for:
 * <ul>
 *   <li>Listing pipelines by user (excluding soft-deleted)
 *   <li>Filtering by status
 *   <li>Verifying ownership
 *   <li>Counting pipelines in a given status (for concurrency control)
 * </ul>
 *
 * <p>All queries respect the soft-delete pattern: {@code deletedAt IS NULL}.
 */
public interface ExportPipelineRepository extends JpaRepository<ExportPipeline, Long> {

    /**
     * Finds all active export pipelines for a given user, ordered by creation time (newest first).
     *
     * @param userId   the username of the user
     * @param pageable pagination parameters
     * @return paginated list of active pipelines
     */
    Page<ExportPipeline> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            @Param("userId") String userId,
            Pageable pageable
    );

    /**
     * Finds active export pipelines for a user filtered by status.
     *
     * @param userId   the username of the user
     * @param status   the export status to filter by
     * @param pageable pagination parameters
     * @return paginated list of matching pipelines
     */
    Page<ExportPipeline> findByUserIdAndStatusAndDeletedAtIsNull(
            @Param("userId") String userId,
            @Param("status") ExportStatus status,
            Pageable pageable
    );

    /**
     * Finds a specific pipeline by ID and user, ensuring ownership.
     * Returns empty if the pipeline does not belong to the user or is deleted.
     *
     * @param id     the pipeline ID
     * @param userId the username of the owner
     * @return the pipeline if found and owned by the user, empty otherwise
     */
    Optional<ExportPipeline> findByIdAndUserIdAndDeletedAtIsNull(
            @Param("id") Long id,
            @Param("userId") String userId
    );

    /**
     * Counts active export pipelines in a given status for a user.
     * Useful for concurrency control (e.g., limiting concurrent running exports).
     *
     * @param userId the username of the user
     * @param status the export status
     * @return count of matching pipelines
     */
    long countByUserIdAndStatusAndDeletedAtIsNull(
            @Param("userId") String userId,
            @Param("status") ExportStatus status
    );

}

