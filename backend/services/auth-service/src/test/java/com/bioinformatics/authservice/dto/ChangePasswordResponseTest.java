package com.bioinformatics.authservice.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChangePasswordResponseTest {

    @Test
    void succeed_createsSuccessResponse() {
        var response = ChangePasswordResponse.succeed();

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Password changed successfully.");
    }

    @Test
    void customResponse_createsResponseWithCustomMessage() {
        var response = new ChangePasswordResponse(false, "Password change failed");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("Password change failed");
    }
}

