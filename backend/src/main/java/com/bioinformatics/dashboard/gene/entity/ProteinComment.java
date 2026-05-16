package com.bioinformatics.dashboard.gene.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "protein_comment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
