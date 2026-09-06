package com.bioinformatics.exportservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

import static com.bioinformatics.shared.models.db.DbSchema.EXPORT_DATA_SCHEMA;

/**
 * JPA entity for denormalized batch job execution progress tracking.
 *
 * <p>While the Spring Batch framework maintains its own {@code BATCH_JOB_EXECUTION} table,
 * this entity provides:
 * <ul>
 *   <li>Direct foreign key from {@link ExportPipeline}
 *   <li>Efficient progress polling without Batch table joins
 *   <li>Chunk-level progress metrics (useful for UI progress bars)
 * </ul>
 *
 * <p>Created once per pipeline job launch; updated by batch listeners after each chunk.
 */
@Entity
@Table(schema = EXPORT_DATA_SCHEMA, name = "export_job_execution")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportJobExecution {

    /**
     * Unique identifier for this job execution progress record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Foreign key to the parent {@link ExportPipeline}.
     * Cascade delete ensures cleanup when pipeline is deleted.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pipeline_id", nullable = false, foreignKey = @ForeignKey(name = "fk_export_job_execution_pipeline"))
    private ExportPipeline pipeline;

    /**
     * Reference to the Spring Batch {@code BATCH_JOB_EXECUTION.id}.
     * Unique to allow efficient lookups: one job execution per pipeline.
     */
    @Column(name = "job_execution_id", nullable = false, unique = true)
    private Long jobExecutionId;

    /**
     * Total number of chunks to process in this batch job.
     * Set during validation phase; may be null until export begins.
     */
    @Column(name = "chunks_total")
    private Integer chunksTotal;

    /**
     * Number of chunks processed so far.
     * Updated after each chunk writes to storage.
     */
    @Column(name = "chunks_processed", nullable = false)
    @Builder.Default
    private Integer chunksProcessed = 0;

    /**
     * Last timestamp when this progress record was updated.
     * Used for detecting stalled jobs or old executions.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * JPA lifecycle hook: set/update the timestamp before persist and update.
     */
    @PrePersist
    private void prePersist() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    /**
     * JPA lifecycle hook: update the timestamp before each update.
     */
    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Convenience method to calculate progress percentage.
     *
     * @return progress as a percentage (0–100), or 0 if total is unknown
     */
    public int getProgressPercent() {
        if (chunksTotal == null || chunksTotal <= 0) {
            return 0;
        }
        return Math.min(100, (chunksProcessed * 100) / chunksTotal);
    }

}

