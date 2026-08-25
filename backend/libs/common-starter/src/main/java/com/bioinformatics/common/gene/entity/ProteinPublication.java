package com.bioinformatics.common.gene.entity;

import jakarta.persistence.*;
import lombok.*;

import static com.bioinformatics.shared.models.db.DbSchema.GENES_SCHEMA;

@Entity
@Table(schema = GENES_SCHEMA, name = "protein_publication")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProteinPublication {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "protein_publication_seq")
    @SequenceGenerator(name = "protein_publication_seq", sequenceName = "protein_publication_seq", allocationSize = 500)
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
