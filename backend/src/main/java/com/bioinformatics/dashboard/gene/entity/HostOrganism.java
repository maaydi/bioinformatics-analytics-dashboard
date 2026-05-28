package com.bioinformatics.dashboard.gene.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity for {@code go_term} table.
 * Gene Ontology terms parsed from DR GO lines.
 *
 * @see <a href="{@docRoot}/documentation/domain-model.md">Domain Model §4</a>
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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "host_organism_seq")
    @SequenceGenerator(name = "host_organism_seq", sequenceName = "host_organism_seq", allocationSize = 500)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "protein_id", nullable = false)
    private ProteinEntry protein;

    @Column(nullable = false)
    private Integer taxid;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;
}
