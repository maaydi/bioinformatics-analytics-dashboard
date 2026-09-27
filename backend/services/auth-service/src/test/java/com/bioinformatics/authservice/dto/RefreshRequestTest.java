package com.bioinformatics.authservice.dto;

import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RefreshRequestTest {

    @Autowired
    private Validator validator;

    @Test
    void validRefreshRequest_passesValidation() {
        var request = new RefreshRequest("valid-refresh-token");
        var violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void emptyRefreshToken_failsValidation() {
        var request = new RefreshRequest("");
        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations.stream()
                .anyMatch(v -> v.getMessage().contains("required")))
                .isTrue();
    }

    @Test
    void nullRefreshToken_failsValidation() {
        var request = new RefreshRequest(null);
        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }
}

