package com.staffengagement.auth.service;

import com.staffengagement.auth.dto.LoginRequest;
import com.staffengagement.auth.dto.RefreshTokenRequest;
import com.staffengagement.auth.exception.TokenRefreshException;
import com.staffengagement.staff.model.RefreshToken;
import com.staffengagement.staff.model.Staff;
import com.staffengagement.staff.model.StaffRole;
import com.staffengagement.staff.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private StaffRepository staffRepository;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private Staff staff;

    @BeforeEach
    void setUp() {
        staff = new Staff();
        ReflectionTestUtils.setField(staff, "id", UUID.randomUUID());
        staff.setEmail("john@example.com");
        staff.setPassword("encoded");
        staff.setRole(StaffRole.STAFF);
        staff.setEmployeeId(UUID.randomUUID());
    }

    @Test
    void login_shouldReturnAuthResponse_withValidCredentials() {
        RefreshToken refreshToken = buildRefreshToken("refresh-token-value");

        when(staffRepository.findByEmail("john@example.com")).thenReturn(Optional.of(staff));
        when(jwtService.generateAccessToken(staff)).thenReturn("access-token-value");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900_000L);
        when(refreshTokenService.createRefreshToken(staff)).thenReturn(refreshToken);

        var response = authService.login(new LoginRequest("john@example.com", "password"));

        assertThat(response.accessToken()).isEqualTo("access-token-value");
        assertThat(response.refreshToken()).isEqualTo("refresh-token-value");
        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.role()).isEqualTo("STAFF");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void login_shouldThrow_whenCredentialsAreInvalid() {
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authService.login(new LoginRequest("john@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_shouldReturnNewAccessToken_withValidRefreshToken() {
        RefreshToken refreshToken = buildRefreshToken("valid-refresh-token");
        refreshToken.setStaff(staff);

        when(refreshTokenService.findByToken("valid-refresh-token")).thenReturn(refreshToken);
        when(refreshTokenService.verifyExpiration(refreshToken)).thenReturn(refreshToken);
        when(jwtService.generateAccessToken(staff)).thenReturn("new-access-token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900_000L);

        var response = authService.refresh(new RefreshTokenRequest("valid-refresh-token"));

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("valid-refresh-token");
    }

    @Test
    void refresh_shouldThrow_whenRefreshTokenIsExpired() {
        when(refreshTokenService.findByToken("expired-token"))
                .thenThrow(new TokenRefreshException("Refresh token has expired or been revoked. Please log in again."));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("expired-token")))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void logout_shouldRevokeAllTokensForStaff() {
        when(staffRepository.findByEmail("john@example.com")).thenReturn(Optional.of(staff));

        authService.logout("john@example.com");

        verify(refreshTokenService).revokeAllTokensForStaff(staff);
    }

    private RefreshToken buildRefreshToken(String tokenValue) {
        RefreshToken rt = new RefreshToken();
        rt.setToken(tokenValue);
        rt.setExpiresAt(Instant.now().plusSeconds(3600));
        rt.setRevoked(false);
        return rt;
    }
}
