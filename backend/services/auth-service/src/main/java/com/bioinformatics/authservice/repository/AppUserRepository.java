package com.bioinformatics.authservice.repository;

import com.bioinformatics.authservice.dto.UserStatus;
import com.bioinformatics.authservice.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link AppUser}.
 *
 * <p>Hard deletes are intentionally disabled: {@link #delete} and {@link #deleteById}
 * perform a soft-delete by setting {@code status = DELETED}. This preserves audit
 * trails and foreign-key integrity with the {@code auth.refresh_token} table.
 *
 * <p>Security impact:
 * <ul>
 *   <li>Deleted users cannot be used for authentication because lifecycle status is checked
 *   <li>Audit trail remains intact for investigations and compliance
 *   <li>Foreign-key data stays valid even after a soft delete
 * </ul>
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /**
     * Finds a user by login username.
     *
     * @param username unique login name
     * @return optional user entity
     */
    Optional<AppUser> findByUsername(String username);

    /**
     * Checks whether a username already exists.
     *
     * @param username login name to validate
     * @return true if user exists
     */
    boolean existsByUsername(String username);

    /**
     * Soft-delete: transitions the user to {@link UserStatus#DELETED}.
     * Physical rows are never removed to preserve audit integrity.
     *
     * @param entity user entity to soft-delete
     */
    @Override
    @Transactional
    @Modifying
    @Query("UPDATE AppUser u SET u.status = 'DELETED', u.updatedAt = CURRENT_TIMESTAMP WHERE u = :entity")
    void delete(@Param("entity") AppUser entity);

    /**
     * Soft-delete by identifier.
     *
     * @param id database user identifier
     */
    @Override
    @Transactional
    @Modifying
    @Query("UPDATE AppUser u SET u.status = 'DELETED', u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    void deleteById(@Param("id") Long id);
}
