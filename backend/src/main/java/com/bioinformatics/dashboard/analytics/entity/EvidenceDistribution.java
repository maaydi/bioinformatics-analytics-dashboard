package com.bioinformatics.dashboard.analytics.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "mv_evidence_distribution")
@RequiredArgsConstructor
@Getter
public class EvidenceDistribution {
    @Id
    private Integer evidenceLevel;
    private String label;
    private Long count;
}
