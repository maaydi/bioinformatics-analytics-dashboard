package com.bioinformatics.dashboard.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    // Hardcoded test secret that is strong enough for HMAC-SHA256
    private final String secret = "abcdefghijklmnopqrstuvwxyz1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private JwtUtil jwtUtil;
    @Mock
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(secret, 3600, 86400);
    }

    @Test
    void generateAccessToken_ReturnsValidToken() {
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getAuthorities()).thenReturn((Collection) List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String token = jwtUtil.generateAccessToken(userDetails);

        assertNotNull(token);
        assertEquals("testuser", jwtUtil.extractUsername(token));
        assertTrue(jwtUtil.isTokenValid(token, userDetails));
        assertFalse(jwtUtil.isRefreshToken(token));
    }

    @Test
    void generateRefreshToken_ReturnsValidToken() {
        when(userDetails.getUsername()).thenReturn("testuser");

        String token = jwtUtil.generateRefreshToken(userDetails);

        assertNotNull(token);
        assertEquals("testuser", jwtUtil.extractUsername(token));
        assertTrue(jwtUtil.isTokenValid(token, userDetails));
        assertTrue(jwtUtil.isRefreshToken(token));
    }

    @Test
    void extractUsername_FromInvalidToken_ThrowsException() {
        assertThrows(Exception.class, () -> jwtUtil.extractUsername("invalid.token.here"));
    }

    @Test
    void isTokenValid_InvalidToken_ReturnsFalse() {
        assertFalse(jwtUtil.isTokenValid("invalid.token.here", userDetails));
    }

    @Test
    void isTokenValid_WrongUser_ReturnsFalse() {
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getAuthorities()).thenReturn((Collection) List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String token = jwtUtil.generateAccessToken(userDetails);

        UserDetails wrongUser = org.mockito.Mockito.mock(UserDetails.class);
        when(wrongUser.getUsername()).thenReturn("wronguser");

        assertFalse(jwtUtil.isTokenValid(token, wrongUser));
    }

    @Test
    void isRefreshToken_AccessToken_ReturnsFalse() {
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getAuthorities()).thenReturn((Collection) List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String token = jwtUtil.generateAccessToken(userDetails);

        assertFalse(jwtUtil.isRefreshToken(token));
    }
}

