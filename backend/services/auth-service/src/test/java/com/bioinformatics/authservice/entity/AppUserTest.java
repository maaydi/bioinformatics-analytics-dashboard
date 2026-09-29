package com.bioinformatics.authservice.entity;

import com.bioinformatics.authservice.dto.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AppUserTest {

    @Test
    void isAccountNonLocked_noLocktime_returnsTrue() {
        var user = AppUser.builder()
                .id(1L)
                .username("alice")
                .password("hashed")
                .role("ROLE_USER")
                .status(UserStatus.ACTIVE)
                .lockedUntil(null)
                .build();

        assertThat(user.isAccountNonLocked()).isTrue();
    }

    @Test
    void isAccountNonLocked_lockExpired_returnsTrue() {
        var user = AppUser.builder()
                .id(1L)
                .username("alice")
                .password("hashed")
                .role("ROLE_USER")
                .status(UserStatus.ACTIVE)
                .lockedUntil(Instant.now().minusSeconds(100))
                .build();

        assertThat(user.isAccountNonLocked()).isTrue();
    }

    @Test
    void isAccountNonLocked_lockNotExpired_returnsFalse() {
        var user = AppUser.builder()
                .id(1L)
                .username("alice")
                .password("hashed")
                .role("ROLE_USER")
                .status(UserStatus.ACTIVE)
                .lockedUntil(Instant.now().plusSeconds(100))
                .build();

        assertThat(user.isAccountNonLocked()).isFalse();
    }

    @Test
    void isEnabled_activeUser_returnsTrue() {
        var user = AppUser.builder()
                .id(1L)
                .username("alice")
                .password("hashed")
                .role("ROLE_USER")
                .status(UserStatus.ACTIVE)
                .build();

        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void isEnabled_createdUser_returnsTrue() {
        var user = AppUser.builder()
                .id(1L)
                .username("alice")
                .password("hashed")
                .role("ROLE_USER")
                .status(UserStatus.CREATED)
                .build();

        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void isEnabled_disabledUser_returnsFalse() {
        var user = AppUser.builder()
                .id(1L)
                .username("alice")
                .password("hashed")
                .role("ROLE_USER")
                .status(UserStatus.DISABLED)
                .build();

        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    void isEnabled_deletedUser_returnsFalse() {
        var user = AppUser.builder()
                .id(1L)
                .username("alice")
                .password("hashed")
                .role("ROLE_USER")
                .status(UserStatus.DELETED)
                .build();

        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    void isAdmin_adminRole_returnsTrue() {
        var admin = AppUser.builder()
                .id(1L)
                .username("admin")
                .password("hashed")
                .role("ROLE_ADMIN")
                .status(UserStatus.ACTIVE)
                .build();

        assertThat(admin.isAdmin()).isTrue();
    }

    @Test
    void isAdmin_userRole_returnsFalse() {
        var user = AppUser.builder()
                .id(1L)
                .username("alice")
                .password("hashed")
                .role("ROLE_USER")
                .status(UserStatus.ACTIVE)
                .build();

        assertThat(user.isAdmin()).isFalse();
    }

    @Test
    void getAuthorities_returnsCorrectRole() {
        var user = AppUser.builder()
                .id(1L)
                .username("alice")
                .password("hashed")
                .role("ROLE_USER")
                .status(UserStatus.ACTIVE)
                .build();

        var authorities = user.getAuthorities();

        assertThat(authorities).hasSize(1);
        assertThat(authorities.stream()
                .map(auth -> auth.getAuthority())
                .toList())
                .contains("ROLE_USER");
    }

    @Test
    void prePersist_setsCreatedAtAndUpdatedAt() {
        var user = AppUser.builder()
                .username("alice")
                .password("hashed")
                .role("ROLE_USER")
                .build();

        assertThat(user.getCreatedAt()).isNull();
        assertThat(user.getUpdatedAt()).isNull();
    }

    @Test
    void defaultFailedAttempts_isZero() {
        var user = AppUser.builder()
                .id(1L)
                .username("alice")
                .password("hashed")
                .role("ROLE_USER")
                .status(UserStatus.ACTIVE)
                .build();

        assertThat(user.getFailedAttempts()).isEqualTo(0);
    }
}

