package com.bioinformatics.dashboard.auth.repository;

import com.bioinformatics.dashboard.auth.entity.AppUser;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link AppUser}.
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    @Override
    @Transactional
    @Modifying
    @Query("UPDATE AppUser u SET u.status = 'DELETED' WHERE u = :entity")
    void delete(@Param("entity") @NonNull AppUser entity);

    @Override
    @Transactional
    @Modifying
    @Query("UPDATE AppUser u SET u.status = 'DELETED' WHERE u.id = :id")
    void deleteById(@Param("id") @NonNull Long id);

    
}
