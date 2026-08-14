package com.bioinformatics.dashboard.security;

import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.auth.repository.AppUserRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;


    private AppUserDetailsService appUserDetailsService;

    @BeforeEach
    void setUp() {
        appUserDetailsService = new AppUserDetailsService(appUserRepository, entityManager);
    }

    @Test
    void loadUserByUsername_UserFound_ReturnsUserDetails() {
        String username = "testuser";
        AppUser appUser = new AppUser();
        appUser.setUsername(username);
        when(entityManager.unwrap(any())).thenReturn(session);
        when(appUserRepository.findByUsername(username)).thenReturn(Optional.of(appUser));

        UserDetails result = appUserDetailsService.loadUserByUsername(username);

        assertNotNull(result);
        assertEquals(username, result.getUsername());
        verify(session).disableFilter("excludeDeletedFilter");
    }

    @Test
    void loadUserByUsername_UserNotFound_ThrowsException() {
        String username = "unknown";

        when(entityManager.unwrap(any())).thenReturn(session);
        when(appUserRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> appUserDetailsService.loadUserByUsername(username));
        verify(session).disableFilter("excludeDeletedFilter");
    }
}
