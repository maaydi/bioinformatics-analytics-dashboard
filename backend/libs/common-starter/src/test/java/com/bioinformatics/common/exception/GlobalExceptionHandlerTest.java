package com.bioinformatics.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldTreatBrokenPipeAsClientAbort() {
        var exception = new HttpMessageNotWritableException(
                "Could not write JSON",
                new IOException("Broken pipe")
        );

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void shouldTreatLocalizedBrokenPipeAsClientAbort() {
        var exception = new HttpMessageNotWritableException(
                "Could not write JSON",
                new IOException("Relais brisé (pipe)")
        );

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void shouldReturnInternalServerErrorForUnexpectedExceptions() {
        var exception = new IllegalStateException("Unexpected failure");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getBody().error()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred. Please contact support.");
        assertThat(response.getBody().timestamp()).isNotNull();
    }
}

