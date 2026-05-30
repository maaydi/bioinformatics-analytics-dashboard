package com.bioinformatics.dashboard.job.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "materialized_view_refresh_log")
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