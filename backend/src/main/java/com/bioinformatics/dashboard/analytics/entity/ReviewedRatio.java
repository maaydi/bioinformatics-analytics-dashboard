package com.bioinformatics.dashboard.analytics.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "mv_reviewed_ratio")
@RequiredArgsConstructor
@Getter
public class ReviewedRatio {
    @Id
    private Boolean reviewed;
    private Long count;
}
