package com.bioinformatics.dashboard.gene.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cross_reference")
@Getter
@Setter
@NoArgsConstructor
public class ProteinComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "protein_id", nullable = false)
    private ProteinEntry protein;

    @Column(nullable = false, length = 50)
    private String commentType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;
}
