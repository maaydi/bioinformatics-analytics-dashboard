package com.bioinformatics.dashboard.gene.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cross_reference")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrossReference {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cross_reference_seq")
    @SequenceGenerator(name = "cross_reference_seq", sequenceName = "cross_reference_seq", allocationSize = 500)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "protein_id", nullable = false)
    private ProteinEntry protein;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String source;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String identifier;

    @Column(name = "secondary_id", columnDefinition = "TEXT")
    private String secondaryId;

    @Column(name = "tertiary_info", columnDefinition = "TEXT")
    private String tertiaryInfo;
}
