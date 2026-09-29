package com.bioinformatics.authservice.dto;

import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ChangePasswordRequestTest {

    @Autowired
    private Validator validator;

    @Test
    void validChangePasswordRequest_passesValidation() {
        var request = new ChangePasswordRequest("currentPassword", "NewPassword123");
        var violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void emptyCurrentPassword_failsValidation() {
        var request = new ChangePasswordRequest("", "NewPassword123");
        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void nullCurrentPassword_failsValidation() {
        var request = new ChangePasswordRequest(null, "NewPassword123");
        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void emptyNewPassword_failsValidation() {
        var request = new ChangePasswordRequest("current", "");
        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void nullNewPassword_failsValidation() {
        var request = new ChangePasswordRequest("current", null);
        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void newPasswordTooShort_failsValidation() {
        var request = new ChangePasswordRequest("current", "Short123");
        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations.stream()
                .anyMatch(v -> v.getMessage().contains("12 characters")))
                .isTrue();
    }

    @Test
    void newPasswordMissingUppercase_failsValidation() {
        var request = new ChangePasswordRequest("current", "lowercase123456");
        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void newPasswordMissingLowercase_failsValidation() {
        var request = new ChangePasswordRequest("current", "UPPERCASE123456");
        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void newPasswordMissingDigit_failsValidation() {
        var request = new ChangePasswordRequest("current", "NoDigitPassword");
        var violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void newPasswordAllRequirementsMetMinLength_passesValidation() {
        var request = new ChangePasswordRequest("current", "ValidPass12345");
        var violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }
}

