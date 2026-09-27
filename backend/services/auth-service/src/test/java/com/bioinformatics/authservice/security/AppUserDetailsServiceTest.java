package com.bioinformatics.authservice.security;

import com.bioinformatics.authservice.dto.UserStatus;
import com.bioinformatics.authservice.entity.AppUser;
import com.bioinformatics.authservice.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    private AppUserDetailsService appUserDetailsService;

    @BeforeEach
    void setUp() {
        appUserDetailsService = new AppUserDetailsService(appUserRepository);
    }

    @Test
    void loadUserByUsername_userExists_returnsUserDetails() {
        var user = AppUser.builder()
                .id(1L)
                .username("alice")
                .password("hashed-password")
                .role("ROLE_USER")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        var result = appUserDetailsService.loadUserByUsername("alice");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getPassword()).isEqualTo("hashed-password");
    }

    @Test
    void loadUserByUsername_userNotFound_throwsUsernameNotFoundException() {
        when(appUserRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserDetailsService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("nonexistent");
    }

    @Test
    void loadUserByUsername_returnsAppUserWithAuthorities() {
        var user = AppUser.builder()
                .id(1L)
                .username("admin")
                .password("hashed-password")
                .role("ROLE_ADMIN")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        var result = appUserDetailsService.loadUserByUsername("admin");

        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .toList())
                .contains("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_disabledUser_stillReturnsUser() {
        var user = AppUser.builder()
                .id(1L)
                .username("alice")
                .password("hashed-password")
                .role("ROLE_USER")
                .status(UserStatus.DISABLED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        var result = appUserDetailsService.loadUserByUsername("alice");

        assertThat(result).isNotNull();
        assertThat(result.isEnabled()).isFalse();
    }
}

