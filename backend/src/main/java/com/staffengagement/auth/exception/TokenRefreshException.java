package com.staffengagement.auth.exception;

public class TokenRefreshException extends RuntimeException {
    public TokenRefreshException(String message) {
        super(message);
    }
}
