package com.bioinformatics.authservice.security;

import com.bioinformatics.shared.models.security.UserPrincipal;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityFilterAspectTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private Session session;

    @Mock
    private Filter filter;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private SecurityFilterAspect securityFilterAspect;

    @BeforeEach
    void setUp() {
        securityFilterAspect = new SecurityFilterAspect(entityManager);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void configureFilters_UserIsAdmin_EnablesFilterExcludingDeletedFalse() {
        UserPrincipal adminUser = mock(UserPrincipal.class);
        when(adminUser.isAdmin()).thenReturn(true);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(adminUser);

        when(entityManager.unwrap(any())).thenReturn(session);
        when(session.enableFilter("excludeDeletedFilter")).thenReturn(filter);

        securityFilterAspect.configureFilters();

        verify(session, times(1)).enableFilter("excludeDeletedFilter");
        verify(filter, times(1)).setParameter("isDeletedExcluded", false);
    }

    @Test
    void configureFilters_UserIsNotAdmin_EnablesFilterExcludingDeletedTrue() {
        UserPrincipal regularUser = mock(UserPrincipal.class);
        when(regularUser.isAdmin()).thenReturn(false);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(regularUser);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter("excludeDeletedFilter")).thenReturn(filter);

        securityFilterAspect.configureFilters();

        verify(session, times(1)).enableFilter("excludeDeletedFilter");
        verify(filter, times(1)).setParameter("isDeletedExcluded", true);
    }

    @Test
    void configureFilters_Unauthenticated_EnablesFilterExcludingDeletedTrue() {
        when(securityContext.getAuthentication()).thenReturn(null);

        when(entityManager.unwrap(Session.class)).thenReturn(session);
        when(session.enableFilter("excludeDeletedFilter")).thenReturn(filter);

        securityFilterAspect.configureFilters();

        verify(session, times(1)).enableFilter("excludeDeletedFilter");
        verify(filter, times(1)).setParameter("isDeletedExcluded", true);
    }
}

