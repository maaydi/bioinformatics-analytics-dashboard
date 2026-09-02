package com.bioinformatics.analyticsservice.materializeviews.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import static com.bioinformatics.shared.models.db.DbSchema.ANALYTICS_SCHEMA;

@Entity
@Table(schema = ANALYTICS_SCHEMA, name = "materialized_view_refresh_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewRefreshLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String jobIdentifier;
    private String viewName;
    private boolean success;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private LocalDateTime executedAt;
}