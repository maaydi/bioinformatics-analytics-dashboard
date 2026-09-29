package com.bioinformatics.authservice.dto;

import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LoginRequestTest {

    @Autowired
    private Validator validator;

    @Test
    void validLoginRequest_passesValidation() {
        var request = new LoginRequest("alice", "password123");
        var violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void emptyUsername_failsValidation() {
        var request = new LoginRequest("", "password123");
        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations.stream()
                .anyMatch(v -> v.getMessage().contains("required")))
                .isTrue();
    }

    @Test
    void nullUsername_failsValidation() {
        var request = new LoginRequest(null, "password123");
        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void shortUsername_failsValidation() {
        var request = new LoginRequest("ab", "password123");
        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations.stream()
                .anyMatch(v -> v.getMessage().contains("between 3 and 50")))
                .isTrue();
    }

    @Test
    void longUsername_failsValidation() {
        var request = new LoginRequest("a".repeat(51), "password123");
        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void emptyPassword_failsValidation() {
        var request = new LoginRequest("alice", "");
        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations.stream()
                .anyMatch(v -> v.getMessage().contains("required")))
                .isTrue();
    }

    @Test
    void nullPassword_failsValidation() {
        var request = new LoginRequest("alice", null);
        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }
}

