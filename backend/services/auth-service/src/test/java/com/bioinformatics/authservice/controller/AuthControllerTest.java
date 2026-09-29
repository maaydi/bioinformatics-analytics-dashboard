package com.bioinformatics.authservice.controller;

import com.bioinformatics.authservice.dto.*;
import com.bioinformatics.authservice.service.AuthService;
import com.bioinformatics.shared.models.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void login_validCredentials_returns200WithTokens() {
        var request = new LoginRequest("alice", "secret");
        var response = new TokenResponse("access", "refresh", 3600, "Bearer");

        when(authService.login(request)).thenReturn(response);

        var result = authController.login(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().accessToken()).isEqualTo("access");
        assertThat(result.getBody().refreshToken()).isEqualTo("refresh");

        verify(authService).login(request);
    }

    @Test
    void login_invalidCredentials_propagatesException() {
        var request = new LoginRequest("alice", "wrong");

        when(authService.login(request))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThatThrownBy(() -> authController.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_validToken_returns200WithNewPair() {
        var request = new RefreshRequest("refresh-token");
        var response = new TokenResponse("new-access", "new-refresh", 3600, "Bearer");

        when(authService.refresh(request)).thenReturn(response);

        var result = authController.refresh(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().accessToken()).isEqualTo("new-access");

        verify(authService).refresh(request);
    }

    @Test
    void refresh_invalidToken_propagatesException() {
        var request = new RefreshRequest("invalid");

        when(authService.refresh(request))
                .thenThrow(new BadCredentialsException("Invalid or expired refresh token"));

        assertThatThrownBy(() -> authController.refresh(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void logout_validUser_returns204NoContent() {
        var user = new UserPrincipal("alice", List.of("ROLE_USER"), "local");

        doNothing().when(authService).logout("alice");

        var result = authController.logout(user);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.getBody()).isNull();

        verify(authService).logout("alice");
    }

    @Test
    void issueServiceToken_adminUser_returns200WithToken() {
        var admin = new UserPrincipal("admin", List.of("ROLE_ADMIN"), "local");
        var response = new TokenResponse("service-token", null, 2592000, "Bearer");

        when(authService.issueServiceToken("admin")).thenReturn(response);

        var result = authController.issueServiceToken(admin);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().accessToken()).isEqualTo("service-token");
        assertThat(result.getBody().refreshToken()).isNull();

        verify(authService).issueServiceToken("admin");
    }

    @Test
    void issueServiceToken_nonAdminUser_throwsForbidden() {
        var user = new UserPrincipal("alice", List.of("ROLE_USER"), "local");

        when(authService.issueServiceToken("alice"))
                .thenThrow(new AccessDeniedException("Only administrators can issue service tokens"));

        assertThatThrownBy(() -> authController.issueServiceToken(user))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only administrators can issue service tokens");
    }

    @Test
    void updatePassword_validRequest_returns200WithSuccess() {
        var request = new ChangePasswordRequest("current", "NewPassword123");
        var user = new UserPrincipal("alice", List.of("ROLE_USER"), "local");
        var response = ChangePasswordResponse.succeed();

        when(authService.updatePassword(request, "alice")).thenReturn(response);

        var result = authController.updatePassword(request, user);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().success()).isTrue();

        verify(authService).updatePassword(request, "alice");
    }

    @Test
    void updatePassword_wrongCurrentPassword_propagatesException() {
        var request = new ChangePasswordRequest("wrong", "NewPassword123");
        var user = new UserPrincipal("alice", List.of("ROLE_USER"), "local");

        when(authService.updatePassword(request, "alice"))
                .thenThrow(new BadCredentialsException("Invalid username or current password"));

        assertThatThrownBy(() -> authController.updatePassword(request, user))
                .isInstanceOf(BadCredentialsException.class);
    }
}

