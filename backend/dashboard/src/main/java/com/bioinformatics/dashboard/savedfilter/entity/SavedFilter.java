package com.bioinformatics.dashboard.savedfilter.entity;

import com.bioinformatics.common.models.gene.GeneSearchRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "saved_filter",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_saved_filter_user_name",
                        columnNames = {"username", "name"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedFilter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false)
    private String owner;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "filter_json", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private GeneSearchRequest filterJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

}
