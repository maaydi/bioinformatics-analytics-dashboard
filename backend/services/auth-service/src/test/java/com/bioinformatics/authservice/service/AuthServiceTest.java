package com.bioinformatics.authservice.service;

import com.bioinformatics.authservice.config.ApplicationProperties;
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

    private static ApplicationProperties.Jwt MOCKED_JWT = new ApplicationProperties.Jwt("", "", "", 3600, 86000, 300, 32);

    private AuthService authService;
    @Mock
    private ApplicationProperties properties;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                authenticationManager,
                userDetailsService,
                jwtService,
                passwordEncoder,
                appUserRepository,
                refreshTokenRepository,
                properties
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
        when(properties.jwt()).thenReturn(MOCKED_JWT);

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
        when(properties.jwt()).thenReturn(MOCKED_JWT);
        when(jwtService.isRefreshToken("old-refresh-token")).thenReturn(true);
        when(jwtService.extractUsername("old-refresh-token")).thenReturn("alice");
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("old-refresh-token", user)).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(storedToken));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh-token");
        when(jwtService.extractExpiration("new-refresh-token")).thenReturn(Instant.now().plusSeconds(86_400));

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
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));

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
        when(properties.jwt()).thenReturn(MOCKED_JWT);

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

        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> authService.issueServiceToken(user.getUsername()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only administrators can issue service tokens");

        verify(jwtService, never()).generateServiceToken(any());
    }

    @Test
    void logout_revokeAllTokensAndClearContext() {
        var user = activeUser();

        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        authService.logout("alice");

        verify(refreshTokenRepository).revokeAllByUserId(user.getId());
    }

    @Test
    void updatePassword_validCurrentPassword_succeeds() {
        var user = activeUser();
        var request = new ChangePasswordRequest("secret", "NewPassword123");

        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassword123")).thenReturn("encoded-new-password");

        var response = authService.updatePassword(request, "alice");

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Password changed successfully.");
        verify(appUserRepository).save(any(AppUser.class));
        verify(refreshTokenRepository).revokeAllByUserId(user.getId());
    }

    @Test
    void refresh_invalidTokenType_throwsUnauthorized() {
        var user = activeUser();
        var request = new RefreshRequest("not-a-refresh-token");

        when(jwtService.isRefreshToken("not-a-refresh-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid or expired refresh token");

        verify(jwtService, never()).extractUsername(any());
    }

    @Test
    void refresh_userNotFound_throwsUnauthorized() {
        var request = new RefreshRequest("refresh-token");

        when(jwtService.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtService.extractUsername("refresh-token")).thenReturn("nonexistent");
        when(appUserRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid or expired refresh token");
    }

    @Test
    void refresh_invalidSignature_throwsUnauthorized() {
        var user = activeUser();
        var request = new RefreshRequest("bad-token");

        when(jwtService.isRefreshToken("bad-token")).thenReturn(true);
        when(jwtService.extractUsername("bad-token")).thenReturn("alice");
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("bad-token", user)).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid or expired refresh token");
    }

    @Test
    void refresh_revokedToken_throwsUnauthorized() {
        var user = activeUser();
        var request = new RefreshRequest("revoked-token");
        var revokedToken = RefreshToken.builder()
                .id(1L)
                .user(user)
                .tokenHash("revoked-hash")
                .expiresAt(Instant.now().plusSeconds(300))
                .revoked(true)
                .build();

        when(jwtService.isRefreshToken("revoked-token")).thenReturn(true);
        when(jwtService.extractUsername("revoked-token")).thenReturn("alice");
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("revoked-token", user)).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid or expired refresh token");
    }

    @Test
    void login_userNotFound_throwsUnauthorized() {
        var request = new LoginRequest("unknown", "password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("unknown", "password"));
        when(userDetailsService.loadUserByUsername("unknown")).thenReturn(
                new org.springframework.security.core.userdetails.User(
                        "unknown", "password", java.util.List.of())
        );
        when(appUserRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void issueServiceToken_adminNotInDatabase_throwsError() {
        var adminFromContext = adminUser();

        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.issueServiceToken("admin"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void issueServiceToken_adminLostAdminStatus_throwsForbidden() {
        var noLongerAdmin = AppUser.builder()
                .id(10L)
                .username("admin")
                .password("bcrypt")
                .role("ROLE_USER")
                .status(UserStatus.ACTIVE)
                .build();

        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(noLongerAdmin));

        assertThatThrownBy(() -> authService.issueServiceToken("admin"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only administrators can issue service tokens");
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

