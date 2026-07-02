package com.staffengagement.shared.exception;

import com.staffengagement.auth.exception.TokenRefreshException;
import com.staffengagement.interaction.exception.InteractionNotFoundException;
import com.staffengagement.interaction.exception.InvalidDateRangeException;
import com.staffengagement.interaction.exception.TaskCreationFailedException;
import com.staffengagement.portfolio.github.EmployeeNotActiveException;
import com.staffengagement.portfolio.github.GitHubApiUnavailableException;
import com.staffengagement.portfolio.github.GitHubNotConfiguredException;
import com.staffengagement.portfolio.github.GitHubRateLimitException;
import com.staffengagement.portfolio.github.GitHubTimeoutException;
import com.staffengagement.portfolio.github.GitHubUserNotFoundException;
import com.staffengagement.portfolio.github.InvalidGitHubUrlException;
import com.staffengagement.shared.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(401, "Invalid email or password", List.of(), LocalDateTime.now()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(403, "Access denied: insufficient permissions", List.of(), LocalDateTime.now()));
    }

    @ExceptionHandler(InvalidGitHubUrlException.class)
    public ResponseEntity<ErrorResponse> handleInvalidGitHubUrl(InvalidGitHubUrlException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, ex.getMessage(), List.of(), LocalDateTime.now()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, ex.getMessage(), List.of(), LocalDateTime.now()));
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<ErrorResponse> handleTokenRefresh(TokenRefreshException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(401, ex.getMessage(), List.of(), LocalDateTime.now()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "Validation failed", errors, LocalDateTime.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, "Validation failed", errors, LocalDateTime.now()));
    }

    @ExceptionHandler(InteractionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleInteractionNotFound(InteractionNotFoundException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Interaction not found");
        String message = ex.getMessage();
        if (message != null && message.contains(": ")) {
            body.put("id", message.substring(message.lastIndexOf(": ") + 2));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(InvalidDateRangeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidDateRange(InvalidDateRangeException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "fromDate must be on or before toDate");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(TaskCreationFailedException.class)
    public ResponseEntity<Map<String, Object>> handleTaskCreationFailed(TaskCreationFailedException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Failed to create follow-up task");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Invalid parameter format: " + ex.getName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, ex.getMessage(), List.of(), LocalDateTime.now()));
    }

    @ExceptionHandler(InactiveStaffException.class)
    public ResponseEntity<ErrorResponse> handleInactiveStaff(InactiveStaffException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, ex.getMessage(), List.of(), LocalDateTime.now()));
    }

    @ExceptionHandler(TaskAssignmentForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleTaskAssignmentForbidden(TaskAssignmentForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(403, ex.getMessage(), List.of(), LocalDateTime.now()));
    }

    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<ErrorResponse> handleInvalidParameter(InvalidParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, ex.getMessage(), List.of(), LocalDateTime.now()));
    }

    @ExceptionHandler(GitHubUserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleGitHubUserNotFound(GitHubUserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, ex.getMessage(), List.of(), LocalDateTime.now()));
    }

    @ExceptionHandler(GitHubRateLimitException.class)
    public ResponseEntity<ErrorResponse> handleGitHubRateLimit(GitHubRateLimitException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse(429, ex.getMessage(), List.of(), LocalDateTime.now()));
    }

    @ExceptionHandler(GitHubApiUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleGitHubApiUnavailable(GitHubApiUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(502, ex.getMessage(), List.of(), LocalDateTime.now()));
    }

    @ExceptionHandler(GitHubTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleGitHubTimeout(GitHubTimeoutException ex) {
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(new ErrorResponse(504, ex.getMessage(), List.of(), LocalDateTime.now()));
    }

    @ExceptionHandler(GitHubNotConfiguredException.class)
    public ResponseEntity<ErrorResponse> handleGitHubNotConfigured(GitHubNotConfiguredException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse(503, ex.getMessage(), List.of(), LocalDateTime.now()));
    }

    @ExceptionHandler(EmployeeNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleEmployeeNotActive(EmployeeNotActiveException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, ex.getMessage(), List.of(), LocalDateTime.now()));
    }

    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleJpaEntityNotFound(jakarta.persistence.EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, ex.getMessage(), List.of(), LocalDateTime.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Internal server error", List.of(), LocalDateTime.now()));
    }
}
