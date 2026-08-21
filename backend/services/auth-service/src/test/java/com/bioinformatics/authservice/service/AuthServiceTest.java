package com.bioinformatics.authservice.service;

import com.bioinformatics.authservice.dto.ChangePasswordRequest;
import com.bioinformatics.authservice.dto.LoginRequest;
import com.bioinformatics.authservice.dto.RefreshRequest;
import com.bioinformatics.authservice.dto.UserStatus;
import com.bioinformatics.authservice.entity.AppUser;
import com.bioinformatics.authservice.entity.RefreshToken;
import com.bioinformatics.authservice.repository.AppUserRepository;
import com.bioinformatics.authservice.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                authenticationManager,
                userDetailsService,
                jwtService,
                passwordEncoder,
                appUserRepository,
                refreshTokenRepository
        );
    }

    @Test
    void login_validCredentials_returnsTokensAndPersistsRefreshToken() {
        var request = new LoginRequest("alice", "secret");
        var user = activeUser();

        when(userDetailsService.loadUserByUsername("alice")).thenReturn(user);
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");
        when(jwtService.extractExpiration("refresh-token")).thenReturn(Instant.now().plusSeconds(86_400));
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(3_600L);

        var response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.expiresIn()).isEqualTo(3_600L);
        assertThat(response.tokenType()).isEqualTo("Bearer");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        var refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        var persistedToken = refreshTokenCaptor.getValue();
        assertThat(persistedToken.getUser()).isEqualTo(user);
        assertThat(persistedToken.getTokenHash()).isNotBlank().isNotEqualTo("refresh-token");
    }

    @Test
    void login_invalidCredentials_throwsUnauthorized() {
        var request = new LoginRequest("alice", "wrong-password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_validToken_returnsNewPairAndRevokesOldToken() {
        var user = activeUser();
        var request = new RefreshRequest("old-refresh-token");
        var storedToken = RefreshToken.builder()
                .id(10L)
                .user(user)
                .tokenHash("stored-hash")
                .expiresAt(Instant.now().plusSeconds(300))
                .revoked(false)
                .build();

        when(jwtService.isRefreshToken("old-refresh-token")).thenReturn(true);
        when(jwtService.extractUsername("old-refresh-token")).thenReturn("alice");
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("old-refresh-token", user)).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(storedToken));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh-token");
        when(jwtService.extractExpiration("new-refresh-token")).thenReturn(Instant.now().plusSeconds(86_400));
        when(jwtService.getAccessTokenExpirySeconds()).thenReturn(3_600L);

        var response = authService.refresh(request);

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(storedToken.isRevoked()).isTrue();

        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refresh_unknownRefreshToken_throwsUnauthorized() {
        var user = activeUser();
        var request = new RefreshRequest("refresh-token");

        when(jwtService.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtService.extractUsername("refresh-token")).thenReturn("alice");
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("refresh-token", user)).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid or expired refresh token");

        verify(jwtService, never()).generateAccessToken(any());
        verify(jwtService, never()).generateRefreshToken(any());
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsUnauthorized() {
        var user = activeUser();
        var request = new ChangePasswordRequest("wrong-current", "ValidPassword123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThatThrownBy(() -> authService.updatePassword(request, user.getUsername()))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or current password");

        verify(appUserRepository, never()).save(any());
        verify(refreshTokenRepository, never()).revokeAllByUserId(any());
    }

    @Test
    void serviceToken_adminRequest_returnsShortLivedJwt() {
        var admin = adminUser();

        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(jwtService.generateServiceToken(admin)).thenReturn("service-token");
        when(jwtService.getServiceTokenExpirySeconds()).thenReturn(300L);

        var response = authService.issueServiceToken(admin.getUsername());

        assertThat(response.accessToken()).isEqualTo("service-token");
        assertThat(response.refreshToken()).isNull();
        assertThat(response.expiresIn()).isEqualTo(300L);
        assertThat(response.tokenType()).isEqualTo("Bearer");

        verify(jwtService).generateServiceToken(admin);
    }

    @Test
    void serviceToken_nonAdminRequest_throwsForbidden() {
        var user = activeUser();

        assertThatThrownBy(() -> authService.issueServiceToken(user.getUsername()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only administrators can issue service tokens");

        verify(appUserRepository, never()).findByUsername(any());
        verify(jwtService, never()).generateServiceToken(any());
    }

    private AppUser activeUser() {
        return AppUser.builder()
                .id(1L)
                .username("alice")
                .password("bcrypt")
                .role("ROLE_USER")
                .status(UserStatus.ACTIVE)
                .build();
    }

    private AppUser adminUser() {
        return AppUser.builder()
                .id(10L)
                .username("admin")
                .password("bcrypt")
                .role("ROLE_ADMIN")
                .status(UserStatus.ACTIVE)
                .build();
    }
}

