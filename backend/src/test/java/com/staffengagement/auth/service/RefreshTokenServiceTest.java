package com.staffengagement.auth.service;

import com.staffengagement.auth.exception.TokenRefreshException;
import com.staffengagement.staff.model.RefreshToken;
import com.staffengagement.staff.model.Staff;
import com.staffengagement.staff.model.StaffRole;
import com.staffengagement.staff.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private Staff staff;

    @BeforeEach
    void setUp() {
        staff = new Staff();
        ReflectionTestUtils.setField(staff, "id", UUID.randomUUID());
        staff.setEmail("user@test.com");
        staff.setPassword("encoded");
        staff.setRole(StaffRole.STAFF);

        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpirationMs", 604_800_000L);
    }

    @Test
    void createRefreshToken_shouldDeleteOldAndCreateNew() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(staff);

        verify(refreshTokenRepository).deleteAllByStaff(staff);
        assertThat(result.getToken()).isNotBlank();
        assertThat(result.getStaff()).isEqualTo(staff);
        assertThat(result.isRevoked()).isFalse();
        assertThat(result.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void verifyExpiration_shouldReturnToken_whenValid() {
        RefreshToken token = new RefreshToken();
        token.setRevoked(false);
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        RefreshToken result = refreshTokenService.verifyExpiration(token);

        assertThat(result).isEqualTo(token);
    }

    @Test
    void verifyExpiration_shouldThrow_whenExpired() {
        RefreshToken token = new RefreshToken();
        token.setRevoked(false);
        token.setExpiresAt(Instant.now().minusSeconds(3600));

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
                .isInstanceOf(TokenRefreshException.class);

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void verifyExpiration_shouldThrow_whenRevoked() {
        RefreshToken token = new RefreshToken();
        token.setRevoked(true);
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
                .isInstanceOf(TokenRefreshException.class);

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void findByToken_shouldReturnToken_whenExists() {
        RefreshToken token = new RefreshToken();
        token.setToken("valid-token");
        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        RefreshToken result = refreshTokenService.findByToken("valid-token");

        assertThat(result.getToken()).isEqualTo("valid-token");
    }

    @Test
    void findByToken_shouldThrow_whenNotFound() {
        when(refreshTokenRepository.findByToken("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.findByToken("invalid"))
                .isInstanceOf(TokenRefreshException.class);
    }

    @Test
    void revokeAllTokensForStaff_shouldDeleteAll() {
        refreshTokenService.revokeAllTokensForStaff(staff);

        verify(refreshTokenRepository).deleteAllByStaff(staff);
    }
}
