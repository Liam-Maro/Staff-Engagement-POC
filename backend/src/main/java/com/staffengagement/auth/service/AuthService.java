package com.staffengagement.auth.service;

import com.staffengagement.auth.dto.AuthResponse;
import com.staffengagement.auth.dto.LoginRequest;
import com.staffengagement.auth.dto.RefreshTokenRequest;
import com.staffengagement.staff.model.RefreshToken;
import com.staffengagement.staff.model.Staff;
import com.staffengagement.staff.repository.StaffRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final StaffRepository staffRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    public AuthService(
            StaffRepository staffRepository,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            AuthenticationManager authenticationManager) {
        this.staffRepository = staffRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        Staff staff = staffRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        String accessToken = jwtService.generateAccessToken(staff);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(staff);

        return AuthResponse.of(
                accessToken,
                refreshToken.getToken(),
                jwtService.getAccessTokenExpirationMs(),
                staff.getEmail(),
                staff.getRole().name()
        );
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.findByToken(request.refreshToken());
        refreshTokenService.verifyExpiration(refreshToken);

        Staff staff = refreshToken.getStaff();
        String newAccessToken = jwtService.generateAccessToken(staff);

        return AuthResponse.of(
                newAccessToken,
                refreshToken.getToken(),
                jwtService.getAccessTokenExpirationMs(),
                staff.getEmail(),
                staff.getRole().name()
        );
    }

    public void logout(String staffEmail) {
        Staff staff = staffRepository.findByEmail(staffEmail)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        refreshTokenService.revokeAllTokensForStaff(staff);
    }
}
