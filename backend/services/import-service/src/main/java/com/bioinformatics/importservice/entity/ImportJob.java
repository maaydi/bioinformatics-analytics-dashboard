package com.bioinformatics.importservice.entity;

import com.bioinformatics.importservice.dto.ImportStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

import static com.bioinformatics.shared.models.db.DbSchema.IMPORT_BATCH_SCHEMA;

/**
 * Entity representing an import job for data ingestion processes.
 * Tracks file import status, progress, error messages, and timing information.
 *
 */
@Entity
@Table(schema = IMPORT_BATCH_SCHEMA, name = "import_job")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportJob {

    /**
     * Unique identifier for the import job.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Current status of the import job.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportStatus status;

    /**
     * Name of the file being imported.
     */
    @Column(name = "file_name", columnDefinition = "TEXT", length = 255)
    private String fileName;

    /**
     * Import strategy (e.g., OVERWRITE, APPEND). Default is OVERWRITE.
     */
    @Column(length = 20, nullable = false)
    @Builder.Default
    private String strategy = "OVERWRITE";

    /**
     * Number of entries detected in the file.
     */
    @Column(name = "entry_count")
    private int entryCount;

    /**
     * Number of records processed so far.
     */
    @Column(name = "records_processed", nullable = false)
    @Builder.Default
    private int recordsProcessed = 0;

    /**
     * Estimated total number of records to process.
     */
    @Column(name = "total_estimated")
    private int totalEstimated;

    /**
     * Duration of the import in milliseconds.
     */
    @Column(name = "duration_ms")
    private long durationMs;

    /**
     * Error message if the import failed.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Timestamp when the job was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when the job completed.
     */
    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Sets the creation timestamp before persisting.
     */
    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

}
