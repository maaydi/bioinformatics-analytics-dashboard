package com.bioinformatics.analyticsservice.providers.postgres.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Immutable entity mapping to the organism occurrences materialized view.
 * Sidesteps expensive GROUP BY operations on main datasets during global analytics calls.
 */
@Entity
@Immutable
@Table(name = "mv_organism_counts")
@IdClass(OrganismCountId.class)
@RequiredArgsConstructor
@Getter
public class OrganismCount {
    @Id
    @Column(name = "organism_name")
    private String organismName;

    @Id
    @Column(name = "taxid")
    private Integer taxid;
    private Integer total;
    private Integer reviewedCount;
    private Integer unreviewedCount;
    @Column(name = "avg_length", columnDefinition = "NUMERIC")
    private Integer avgLength;
}
