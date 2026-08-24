package com.bioinformatics.common.gene.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(schema = "public", name = "protein_comment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProteinComment {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "protein_comment_seq")
    @SequenceGenerator(name = "protein_comment_seq", sequenceName = "protein_comment_seq", allocationSize = 500)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "protein_id", nullable = false)
    private ProteinEntry protein;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String commentType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;
}
