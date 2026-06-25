package com.staffengagement.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staffengagement.auth.dto.AuthResponse;
import com.staffengagement.auth.dto.LoginRequest;
import com.staffengagement.auth.dto.RefreshTokenRequest;
import com.staffengagement.auth.service.AuthService;
import com.staffengagement.auth.service.JwtService;
import com.staffengagement.shared.config.JwtAuthenticationFilter;
import com.staffengagement.shared.config.SecurityConfig;
import com.staffengagement.staff.repository.StaffRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AuthService authService;
    @MockitoBean JwtService jwtService;
    @MockitoBean StaffRepository staffRepository;

    @Test
    void login_shouldReturn200_withValidCredentials() throws Exception {
        AuthResponse response = AuthResponse.of("access-token", "refresh-token", 900_000L, "john@example.com", "STAFF", "staff-uuid-123");
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("john@example.com", "password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("STAFF"));
    }

    @Test
    void login_shouldReturn400_whenEmailIsBlank() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("", "password"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_shouldReturn400_whenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("not-an-email", "password"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_shouldReturn401_whenCredentialsAreInvalid() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("john@example.com", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_shouldReturn200_withValidRefreshToken() throws Exception {
        AuthResponse response = AuthResponse.of("new-access-token", "refresh-token", 900_000L, "john@example.com", "STAFF", "staff-uuid-123");
        when(authService.refresh(any(RefreshTokenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("refresh-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    @WithMockUser(username = "john@example.com")
    void logout_shouldReturn204() throws Exception {
        mockMvc.perform(post("/api/auth/logout").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
