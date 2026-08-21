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
 * <p>All writes use the PRIMARY datasource (auth-service has no read replica
 * routing requirement — it is write-heavy by nature).
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revokes all active refresh tokens for a user — used on logout and
     * password change to invalidate all sessions.
     */
    @Transactional
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.user.id = :userId AND t.revoked = false")
    void revokeAllByUserId(@Param("userId") Long userId);

    /**
     * Purges expired tokens older than the given threshold.
     * Intended for a scheduled maintenance job to keep the table compact.
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :threshold")
    int deleteExpiredBefore(@Param("threshold") Instant threshold);
}

