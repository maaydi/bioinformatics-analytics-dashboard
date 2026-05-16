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

    @Column(length = 20)
    private String pubmedId;

    @Column(length = 200)
    private String doi;

    @Column(columnDefinition = "TEXT")
    private String authors;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(length = 300)
    private String journal;
}
