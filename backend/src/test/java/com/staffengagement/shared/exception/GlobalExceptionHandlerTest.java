package com.staffengagement.shared.exception;

import com.staffengagement.auth.exception.TokenRefreshException;
import com.staffengagement.interaction.exception.InteractionNotFoundException;
import com.staffengagement.interaction.exception.InvalidDateRangeException;
import com.staffengagement.interaction.exception.TaskCreationFailedException;
import com.staffengagement.shared.dto.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleBadCredentials_shouldReturn401() {
        var result = handler.handleBadCredentials(new BadCredentialsException("Bad"));
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getBody().status()).isEqualTo(401);
    }

    @Test
    void handleAccessDenied_shouldReturn403() {
        var result = handler.handleAccessDenied(new AccessDeniedException("Denied"));
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getBody().status()).isEqualTo(403);
    }

    @Test
    void handleIllegalArgument_shouldReturn409() {
        var result = handler.handleIllegalArgument(new IllegalArgumentException("conflict"));
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getBody().message()).isEqualTo("conflict");
    }

    @Test
    void handleTokenRefresh_shouldReturn401() {
        var result = handler.handleTokenRefresh(new TokenRefreshException("expired"));
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(result.getBody().message()).isEqualTo("expired");
    }

    @SuppressWarnings("unchecked")
    @Test
    void handleConstraintViolation_shouldReturn400WithErrors() {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("fieldName");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be null");

        var ex = new ConstraintViolationException(Set.of(violation));
        var result = handler.handleConstraintViolation(ex);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody().errors()).isNotEmpty();
    }

    @Test
    void handleInteractionNotFound_shouldReturn404() {
        UUID id = UUID.randomUUID();
        var result = handler.handleInteractionNotFound(new InteractionNotFoundException(id));
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody().get("error")).isEqualTo("Interaction not found");
    }

    @Test
    void handleInvalidDateRange_shouldReturn400() {
        var result = handler.handleInvalidDateRange(new InvalidDateRangeException());
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody().get("error")).isEqualTo("fromDate must be on or before toDate");
    }

    @Test
    void handleTaskCreationFailed_shouldReturn500() {
        var result = handler.handleTaskCreationFailed(new TaskCreationFailedException("fail"));
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void handleTypeMismatch_shouldReturn400() {
        var ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("id");
        var result = handler.handleTypeMismatch(ex);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleNotFound_shouldReturn404() {
        var result = handler.handleNotFound(new EntityNotFoundException("not found"));
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(result.getBody().message()).isEqualTo("not found");
    }

    @Test
    void handleInactiveStaff_shouldReturn400() {
        var result = handler.handleInactiveStaff(new InactiveStaffException("inactive"));
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleTaskAssignmentForbidden_shouldReturn403() {
        var result = handler.handleTaskAssignmentForbidden(new TaskAssignmentForbiddenException("forbidden"));
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handleInvalidParameter_shouldReturn400() {
        var result = handler.handleInvalidParameter(new InvalidParameterException("bad param"));
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleGeneric_shouldReturn500() {
        var result = handler.handleGeneric(new RuntimeException("unexpected"));
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result.getBody().status()).isEqualTo(500);
    }
}
