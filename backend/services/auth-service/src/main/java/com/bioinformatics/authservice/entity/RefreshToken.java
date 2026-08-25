package com.bioinformatics.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

import static com.bioinformatics.shared.models.db.DbSchema.AUTH_SCHEMA;

/**
 * Persisted refresh-token record for the {@code auth.refresh_token} table.
 *
 * <p>Only the SHA-256 hash of the raw token is stored ({@link #tokenHash}).
 * The raw token is sent to the client once and never persisted, which limits
 * the blast radius of a database breach.
 *
 * <p>Revocation is handled by setting {@link #revoked} to {@code true} on logout.
 * Expired rows can be purged by a scheduled job without affecting active sessions.
 */
@Entity
@Table(schema = AUTH_SCHEMA, name = "refresh_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "refresh_token_seq")
    @SequenceGenerator(name = "refresh_token_seq", sequenceName = "auth.refresh_token_id_seq", allocationSize = 1)
    private Long id;

    /**
     * Owning user.  Loaded lazily — we rarely need the full user when checking a token.
     * Cascade is intentionally omitted: token lifecycle is managed independently.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /**
     * SHA-256 hex digest of the raw refresh token.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 100)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    // ── Domain helpers ────────────────────────────────────────────────────────

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }
}

