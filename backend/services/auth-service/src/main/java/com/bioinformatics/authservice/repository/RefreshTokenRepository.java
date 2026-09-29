package com.bioinformatics.authservice.repository;

import com.bioinformatics.authservice.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link RefreshToken}.
 *
 * <p>All writes use the primary auth datasource. Refresh tokens are persisted as SHA-256
 * hashes, never raw values, to minimize damage from a database leak.
 *
 * <p>Lifecycle behavior:
 * <ul>
 *   <li>On login: a new refresh-token record is created
 *   <li>On refresh: old token is revoked and a new one is issued
 *   <li>On logout/password reset: all tokens for the user are revoked
 *   <li>Expired tokens can be purged by a scheduled maintenance task
 * </ul>
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Finds a refresh token by its SHA-256 hash.
     *
     * @param tokenHash hashed refresh token
     * @return persisted refresh token record if found
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revokes all active refresh tokens for a user.
     *
     * <p>Used during logout and password change to invalidate all active sessions.
     *
     * @param userId user whose tokens must be revoked
     */
    @Transactional
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.user.id = :userId AND t.revoked = false")
    void revokeAllByUserId(@Param("userId") Long userId);

    /**
     * Removes expired refresh tokens older than the provided threshold.
     *
     * <p>Intended for a scheduled maintenance job that keeps the auth table compact
     * while preserving audit data for active sessions.
     *
     * @param threshold expiration threshold
     * @return number of deleted rows
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :threshold")
    int deleteExpiredBefore(@Param("threshold") Instant threshold);
}
