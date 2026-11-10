package com.waffles.harbor.security;

import com.waffles.harbor.auth.Role;
import com.waffles.harbor.user.User;
import com.waffles.harbor.user.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;
    private User user;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);

        user = User.builder()
                .userId(UUID.randomUUID())
                .username("happy")
                .email("happy@gmail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenHeaderIsMissing() throws Exception {
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userRepository);
    }

    @Test
    void shouldContinueWhenHeaderIsNotBearer() throws Exception {
        request.addHeader("Authorization", "Basic abcdef");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtService, userRepository);
    }

    @Test
    void shouldAuthenticateUserWithValidToken() throws Exception {
        request.addHeader("Authorization", "Bearer token");

        when(jwtService.extractSubject("token")).thenReturn(user.getUserId().toString());
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("token", user)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(user, authentication.getPrincipal());
        assertEquals(user.getAuthorities(), authentication.getAuthorities());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldNotAuthenticateWhenTokenIsInvalid() throws Exception {
        request.addHeader("Authorization", "Bearer token");

        when(jwtService.extractSubject("token")).thenReturn(user.getUserId().toString());
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("token", user)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenCannotBeParsed() throws Exception {
        request.addHeader("Authorization", "Bearer bad-token");

        when(jwtService.extractSubject("bad-token")).thenThrow(new JwtException("bad"));

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertEquals("Invalid or expired token", response.getContentAsString());
        verify(filterChain, never()).doFilter(any(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldReturnUnauthorizedWhenSubjectIsNotAUserId() throws Exception {
        request.addHeader("Authorization", "Bearer token");

        when(jwtService.extractSubject("token")).thenReturn("not-a-uuid");

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(userRepository, never()).findById(any());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void shouldReturnUnauthorizedWhenUserNoLongerExists() throws Exception {
        request.addHeader("Authorization", "Bearer token");

        when(jwtService.extractSubject("token")).thenReturn(user.getUserId().toString());
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldSkipAuthenticationWhenAlreadyAuthenticated() throws Exception {
        request.addHeader("Authorization", "Bearer token");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of())
        );

        when(jwtService.extractSubject("token")).thenReturn(user.getUserId().toString());

        filter.doFilterInternal(request, response, filterChain);

        verify(userRepository, never()).findById(any());
        verify(jwtService, never()).isTokenValid(any(), any());
        verify(filterChain).doFilter(request, response);
    }
}
