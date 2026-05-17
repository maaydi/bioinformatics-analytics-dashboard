package com.bioinformatics.dashboard.gene.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "protein_publication")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProteinPublication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "protein_id", nullable = false)
    private ProteinEntry protein;

    private Short refNumber;

    @Column(columnDefinition = "TEXT")
    private String pubmedId;

    @Column(columnDefinition = "TEXT")
    private String doi;

    @Column(columnDefinition = "TEXT")
    private String authors;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String journal;
}
