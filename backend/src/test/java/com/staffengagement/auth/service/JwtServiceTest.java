package com.staffengagement.auth.service;

import com.staffengagement.staff.model.Staff;
import com.staffengagement.staff.model.StaffRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 900_000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", EXPIRATION_MS);
    }

    @Test
    void generateAccessToken_shouldReturnNonBlankToken() {
        String token = jwtService.generateAccessToken(buildStaff("john@example.com"));
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUsername_shouldReturnEmailFromToken() {
        Staff staff = buildStaff("jane@example.com");
        String token = jwtService.generateAccessToken(staff);
        assertThat(jwtService.extractUsername(token)).isEqualTo("jane@example.com");
    }

    @Test
    void isTokenValid_shouldReturnTrue_forValidTokenAndMatchingUser() {
        Staff staff = buildStaff("valid@example.com");
        String token = jwtService.generateAccessToken(staff);
        assertThat(jwtService.isTokenValid(token, staff)).isTrue();
    }

    @Test
    void isTokenValid_shouldReturnFalse_forDifferentUser() {
        Staff staff1 = buildStaff("user1@example.com");
        Staff staff2 = buildStaff("user2@example.com");
        String token = jwtService.generateAccessToken(staff1);
        assertThat(jwtService.isTokenValid(token, staff2)).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalse_forExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", -1L);
        Staff staff = buildStaff("expired@example.com");
        String token = jwtService.generateAccessToken(staff);
        assertThat(jwtService.isTokenValid(token, staff)).isFalse();
    }

    @Test
    void extractUsername_shouldReturnFalse_forInvalidToken() {
        Staff staff = buildStaff("any@example.com");
        assertThat(jwtService.isTokenValid("not.a.valid.token", staff)).isFalse();
    }

    private Staff buildStaff(String email) {
        Staff staff = new Staff();
        ReflectionTestUtils.setField(staff, "id", UUID.randomUUID());
        staff.setEmail(email);
        staff.setPassword("encoded");
        staff.setRole(StaffRole.STAFF);
        return staff;
    }
}
