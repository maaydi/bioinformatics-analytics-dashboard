package com.bioinformatics.exportservice.entity;

import com.bioinformatics.common.models.gene.GeneSearchRequest;
import com.bioinformatics.exportservice.dto.ExportFormat;
import com.bioinformatics.exportservice.dto.ExportStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

import static com.bioinformatics.shared.models.db.DbSchema.EXPORT_DATA_SCHEMA;

/**
 * JPA entity representing an export pipeline configuration and its execution state.
 *
 * <p>An export pipeline encapsulates:
 * <ul>
 *   <li>Filter criteria (as JSONB for dynamic queryability)
 *   <li>Format specification (CSV, TSV, JSON, EXCEL)
 *   <li>Selected export fields (as ordered JSONB array)
 *   <li>Execution tracking (status, progress, file location, timing)
 * </ul>
 *
 * <p>Pipelines can be retried or deleted (soft-delete via {@code deletedAt}).
 * The lifecycle stages are: QUEUED → RUNNING → COMPLETED/FAILED → (optionally deleted).
 */
@Entity
@Table(schema = EXPORT_DATA_SCHEMA, name = "export_pipeline")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportPipeline {

    /**
     * Unique identifier for the export pipeline.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Username of the user who created this pipeline.
     * Stored as username (not ID) for resilience across auth service changes.
     */
    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    /**
     * Human-readable name for this export pipeline.
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Optional description of the export's purpose.
     */
    @Column(length = 500)
    private String description;

    /**
     * Serialized {@code GeneSearchRequest} containing filter criteria.
     * Stored as JSONB for query flexibility and indexing.
     */
    @Column(name = "filter_json", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private GeneSearchRequest filterJson;

    /**
     * Export format: CSV, TSV, JSON, or EXCEL.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ExportFormat format;

    /**
     * Ordered list of field names to include in the export.
     * Stored as JSONB array for query flexibility.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "field_schema", nullable = false, columnDefinition = "JSONB")
    private JsonNode fieldSchema;

    /**
     * Current execution status of the pipeline.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ExportStatus status = ExportStatus.QUEUED;

    /**
     * Estimated number of rows that will be exported (pre-check via count query).
     */
    @Column(name = "estimated_rows")
    private Long estimatedRows;

    /**
     * Actual number of rows written to the export file.
     */
    @Column(name = "actual_rows")
    private Long actualRows;

    /**
     * Relative or absolute path where the final export file is stored.
     */
    @Column(name = "file_path", length = 500)
    private String filePath;

    /**
     * Size of the export file in bytes.
     */
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    /**
     * Error message if the pipeline failed (null for successful exports).
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Reference to the Spring Batch {@code BATCH_JOB_EXECUTION.id}.
     * Used to correlate with batch framework for job control (stop, resume).
     */
    @Column(name = "job_execution_id")
    private Long jobExecutionId;

    /**
     * Timestamp when the pipeline was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when the pipeline started execution.
     */
    @Column(name = "started_at")
    private Instant startedAt;

    /**
     * Timestamp when the pipeline completed (either successfully or with error).
     */
    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Timestamp when the pipeline was soft-deleted.
     * {@code null} means the pipeline is active; set to a value to mark for deletion.
     * Physical files and DB records are cleaned up after a retention period (e.g., 30 days).
     */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Total duration of pipeline execution in milliseconds.
     * Calculated as ({@code completedAt} - {@code startedAt}).
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * JPA lifecycle hook: set creation timestamp before first persist.
     */
    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Convenience method to check if this pipeline is in a terminal state.
     *
     * @return true if status is COMPLETED, FAILED, or CANCELLED
     */
    public boolean isTerminal() {
        return status == ExportStatus.COMPLETED
                || status == ExportStatus.FAILED
                || status == ExportStatus.CANCELLED;
    }

    /**
     * Convenience method to check if this pipeline is soft-deleted.
     *
     * @return true if deletedAt is not null
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

}

