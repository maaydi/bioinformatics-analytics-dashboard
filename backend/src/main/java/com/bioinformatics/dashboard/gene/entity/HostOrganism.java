package com.bioinformatics.dashboard.gene.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity for {@code go_term} table.
 * Gene Ontology terms parsed from DR GO lines.
 *
 * @see documentation/domain-model.md §4
 */
@Entity
@Table(name = "host_organism")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostOrganism {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "protein_id", nullable = false)
    private ProteinEntry protein;

    @Column(nullable = false)
    private Integer taxid;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;
}
