package com.bioinformatics.dashboard.auth.service;

import com.bioinformatics.dashboard.auth.dto.LoginRequest;
import com.bioinformatics.dashboard.auth.dto.RefreshRequest;
import com.bioinformatics.dashboard.auth.entity.AppUser;
import com.bioinformatics.dashboard.auth.repository.AppUserRepository;
import com.bioinformatics.dashboard.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    UserDetailsService userDetailsService;

    @Mock
    JwtUtil jwtUtil;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    AppUserRepository userRepository;

    AuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        authService = new AuthService(authenticationManager, userDetailsService, jwtUtil, passwordEncoder, userRepository);
        var f = AuthService.class.getDeclaredField("accessTokenExpirySeconds");
        f.setAccessible(true);
        f.setLong(authService, 3600L);
    }

    @Test
    void login_shouldReturnTokenResponse_whenCredentialsValid() {
        var req = new LoginRequest("alice", "secret");
        var userDetails = User.withUsername("alice").password("secret").roles("USER").build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(org.springframework.security.core.Authentication.class));
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);
        when(jwtUtil.generateAccessToken(userDetails)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(userDetails)).thenReturn("refresh-token");

        var resp = authService.login(req);

        assertThat(resp).isNotNull();
        assertThat(resp.accessToken()).isEqualTo("access-token");
        assertThat(resp.refreshToken()).isEqualTo("refresh-token");
        assertThat(resp.expiresIn()).isEqualTo(3600L);
        assertThat(resp.tokenType()).isEqualTo("Bearer");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtUtil).generateAccessToken(userDetails);
        verify(jwtUtil).generateRefreshToken(userDetails);
    }

    @Test
    void refresh_shouldReturnNewTokens_whenRefreshTokenValid() {
        var currentUser = AppUser.builder().username("test").build();
        var req = new RefreshRequest("r-token");

        when(jwtUtil.isRefreshToken("r-token")).thenReturn(true);
        when(jwtUtil.extractUsername("r-token")).thenReturn("bob");
        UserDetails userDetails = User.withUsername("bob").password("x").roles("USER").build();
        when(userDetailsService.loadUserByUsername("bob")).thenReturn(userDetails);
        when(jwtUtil.isTokenValid("r-token", userDetails)).thenReturn(true);
        when(jwtUtil.generateAccessToken(userDetails)).thenReturn("new-access");
        when(jwtUtil.generateRefreshToken(userDetails)).thenReturn("new-refresh");

        var resp = authService.refresh(req, currentUser);

        assertThat(resp.accessToken()).isEqualTo("new-access");
        assertThat(resp.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void refresh_shouldThrowBadCredentials_whenTokenNotRefresh() {
        var currentUser = AppUser.builder().username("test").build();

        var req = new RefreshRequest("bad-token");
        when(jwtUtil.isRefreshToken("bad-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(req, currentUser))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
    }
}

