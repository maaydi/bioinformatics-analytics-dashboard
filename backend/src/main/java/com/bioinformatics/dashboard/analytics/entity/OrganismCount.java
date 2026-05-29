package com.bioinformatics.dashboard.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "mv_organism_counts")
@RequiredArgsConstructor
@Getter
public class OrganismCount {
    @Id
    private String organismName;
    private Integer taxid;
    private Integer total;
    private Integer reviewedCount;
    private Integer unreviewedCount;
    @Column(name = "avg_length", columnDefinition = "NUMERIC")
    private Integer avgLength;
}
