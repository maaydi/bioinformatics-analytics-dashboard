package com.bioinformatics.dashboard.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bioinformatics.dashboard.auth.entity.AppUser;

/**
 * Spring Data JPA repository for {@link AppUser}.
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);
}
