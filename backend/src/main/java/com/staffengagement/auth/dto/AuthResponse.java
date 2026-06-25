package com.staffengagement.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String email,
        String role,
        String staffId
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, String email, String role, String staffId) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, email, role, staffId);
    }
}
