package com.waffles.harbor.security;

import com.waffles.harbor.auth.Role;
import com.waffles.harbor.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        String encodedKey = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("test-secret-key-that-is-long-enough-123456".getBytes(StandardCharsets.UTF_8));

        ReflectionTestUtils.setField(jwtService, "securityKey", encodedKey);

        user = User.builder()
                .userId(UUID.randomUUID())
                .username("happy")
                .email("happy@gmail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();
    }

    @Test
    void shouldGenerateTokenWithUserSubject() {
        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);
        assertEquals(user.getUserId().toString(), jwtService.extractSubject(token));
    }

    @Test
    void shouldSetExpirationInTheFuture() {
        String token = jwtService.generateToken(user);

        Date expiration = jwtService.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    void shouldExtractClaimsWithCustomResolver() {
        String token = jwtService.generateToken(user);

        String subject = jwtService.extractClaim(token, Claims::getSubject);

        assertEquals(user.getUserId().toString(), subject);
    }

    @Test
    void shouldValidateTokenForMatchingUser() {
        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void shouldRejectTokenForDifferentUser() {
        String token = jwtService.generateToken(user);

        User otherUser = User.builder()
                .userId(UUID.randomUUID())
                .username("other")
                .email("other@gmail.com")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        assertFalse(jwtService.isTokenValid(token, otherUser));
    }

    @Test
    void shouldRejectMalformedToken() {
        assertThrows(JwtException.class, () -> jwtService.extractAllClaims("not-a-real-token"));
    }
}
