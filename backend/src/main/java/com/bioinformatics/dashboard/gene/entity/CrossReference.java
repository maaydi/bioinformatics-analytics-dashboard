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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "protein_id", nullable = false)
    private ProteinEntry proteinEntry;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(nullable = false, length = 100)
    private String identifier;

    @Column(name = "secondary_id", length = 100)
    private String secondaryId;

    @Column(name = "tertiary_info", length = 200)
    private String tertiaryInfo;
}
