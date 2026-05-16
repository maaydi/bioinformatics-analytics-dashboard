package com.bioinformatics.dashboard.gene.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity for {@code protein_feature} table.
 * Annotated sequence regions (CHAIN, DOMAIN, SIGNAL, BINDING …) from FT lines.
 *
 * @see documentation/domain-model.md §7
 */
@Entity
@Table(name = "protein_feature")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProteinFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "protein_id", nullable = false)
    private ProteinEntry proteinEntry;

    @Column(name = "feature_type", nullable = false, length = 30)
    private String featureType;

    @Column(name = "start_pos")
    private Integer startPos;

    @Column(name = "end_pos")
    private Integer endPos;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "feature_id", length = 20)
    private String featureId;

    @Column(columnDefinition = "TEXT")
    private String evidence;
}
