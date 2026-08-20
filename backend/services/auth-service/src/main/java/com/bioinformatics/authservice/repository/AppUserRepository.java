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
 * perform a soft-delete by setting {@code status = DELETED}.  This preserves audit
 * trails and FK integrity with the {@code auth.refresh_token} table.
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    /**
     * Soft-delete: transitions the user to {@link UserStatus#DELETED}.
     * Physical rows are never removed to preserve audit integrity.
     */
    @Override
    @Transactional
    @Modifying
    @Query("UPDATE AppUser u SET u.status = 'DELETED', u.updatedAt = CURRENT_TIMESTAMP WHERE u = :entity")
    void delete(@Param("entity") AppUser entity);

    @Override
    @Transactional
    @Modifying
    @Query("UPDATE AppUser u SET u.status = 'DELETED', u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    void deleteById(@Param("id") Long id);
}

