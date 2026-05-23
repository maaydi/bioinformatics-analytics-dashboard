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
@ToString
public class ProteinFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "protein_feature_seq")
    @SequenceGenerator(name = "protein_feature_seq", sequenceName = "protein_feature_seq", allocationSize = 500)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "protein_id", nullable = false)
    private ProteinEntry protein;

    @Column(name = "feature_type", nullable = false, columnDefinition = "TEXT")
    private String featureType;

    @Column(name = "start_pos")
    private Integer startPos;

    @Column(name = "end_pos")
    private Integer endPos;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "feature_id", columnDefinition = "TEXT")
    private String featureId;

    @Column(columnDefinition = "TEXT")
    private String evidence;
}
