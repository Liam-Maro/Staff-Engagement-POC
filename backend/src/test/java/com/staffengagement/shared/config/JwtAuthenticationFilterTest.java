package com.staffengagement.shared.config;

import com.staffengagement.auth.service.JwtService;
import com.staffengagement.staff.model.Staff;
import com.staffengagement.staff.model.StaffRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueFilterChain_whenNoAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldContinueFilterChain_whenAuthHeaderNotBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldContinueFilterChain_whenTokenExtractionFails() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token");
        when(jwtService.extractUsername("invalid.token")).thenThrow(new RuntimeException("parse error"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldSetAuthentication_whenTokenIsValid() throws Exception {
        Staff staff = new Staff();
        ReflectionTestUtils.setField(staff, "id", UUID.randomUUID());
        staff.setEmail("user@test.com");
        staff.setPassword("encoded");
        staff.setRole(StaffRole.STAFF);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token");
        when(jwtService.extractUsername("valid.token")).thenReturn("user@test.com");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(staff);
        when(jwtService.isTokenValid("valid.token", staff)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("user@test.com");
    }

    @Test
    void shouldNotSetAuthentication_whenTokenIsInvalid() throws Exception {
        Staff staff = new Staff();
        ReflectionTestUtils.setField(staff, "id", UUID.randomUUID());
        staff.setEmail("user@test.com");
        staff.setPassword("encoded");
        staff.setRole(StaffRole.STAFF);

        when(request.getHeader("Authorization")).thenReturn("Bearer expired.token");
        when(jwtService.extractUsername("expired.token")).thenReturn("user@test.com");
        when(userDetailsService.loadUserByUsername("user@test.com")).thenReturn(staff);
        when(jwtService.isTokenValid("expired.token", staff)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldNotSetAuthentication_whenUsernameIsNull() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer some.token");
        when(jwtService.extractUsername("some.token")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
