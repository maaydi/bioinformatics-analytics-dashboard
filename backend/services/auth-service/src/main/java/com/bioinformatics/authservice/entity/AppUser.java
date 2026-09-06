package com.bioinformatics.authservice.entity;

import com.bioinformatics.authservice.dto.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.bioinformatics.shared.models.db.DbSchema.AUTH_SCHEMA;
import static com.bioinformatics.shared.models.db.DbSchema.GENES_SCHEMA;

/**
 * JPA entity for the {@code auth.app_user} table.
 *
 * <p>Implements {@link UserDetails} so it can be handed directly to Spring Security
 * without an intermediate projection.  The {@code auth} schema isolates this table
 * from all other service schemas.
 *
 * <p>Why {@code SEQUENCE} not {@code IDENTITY}: Hibernate batch inserts require
 * pre-allocated IDs, and SEQUENCE allows that while IDENTITY forces a round-trip
 * per insert.  Even though auth writes are rare, we keep the convention consistent
 * across all entities in this service.
 */
@Entity
@Table(schema = AUTH_SCHEMA, name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FilterDef(
        name = "excludeDeletedFilter",
        parameters = @ParamDef(name = "isDeletedExcluded", type = Boolean.class)
)
@Filter(
        name = "excludeDeletedFilter",
        condition = "(:isDeletedExcluded = true AND status <> 'DELETED' OR :isDeletedExcluded = false)"
)
public class AppUser implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "app_user_seq")
    @SequenceGenerator(name = "app_user_seq",schema = AUTH_SCHEMA, sequenceName = "app_user_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * BCrypt hash — never the raw password.
     */
    @Column(nullable = false, length = 255)
    private String password;

    /**
     * Spring Security role string, e.g. {@code ROLE_USER} or {@code ROLE_ADMIN}.
     * Stored as a plain VARCHAR so future multi-role RBAC can be added without a
     * schema change.
     */
    @Column(nullable = false, length = 20)
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    /**
     * Number of consecutive failed login attempts since last successful login.
     */
    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private int failedAttempts = 0;

    /**
     * When set, the account is locked until this instant.
     * {@code null} means the account is not locked.
     */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    private void prePersist() {
        var now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null) status = UserStatus.CREATED;
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }

    // ── UserDetails ───────────────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public boolean isAccountNonLocked() {
        return lockedUntil == null || Instant.now().isAfter(lockedUntil);
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE || status == UserStatus.CREATED;
    }

    // ── Domain helpers ────────────────────────────────────────────────────────

    public boolean isAdmin() {
        return getAuthorities().stream()
                .anyMatch(auth -> Objects.equals(auth.getAuthority(), "ROLE_ADMIN"));
    }
}

