package com.myedspace.cuj.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit test, no Spring context — JwtService has no framework dependencies worth booting one for. */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-that-is-long-enough-for-hmac-sha-256";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60);
    }

    @Test
    void issuedTokenValidatesBackToTheSameStudentId() {
        String token = jwtService.issueToken(42L, "student@example.com");

        assertThat(jwtService.validateAndGetStudentId(token)).contains(42L);
    }

    @Test
    void garbageTokenIsRejected() {
        assertThat(jwtService.validateAndGetStudentId("not-a-jwt-at-all")).isEmpty();
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.issueToken(1L, "student@example.com");
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        assertThat(jwtService.validateAndGetStudentId(tampered)).isEmpty();
    }

    @Test
    void tokenSignedWithADifferentSecretIsRejected() {
        JwtService otherService = new JwtService("a-completely-different-unit-test-secret-key-value", 60);
        String token = otherService.issueToken(1L, "student@example.com");

        Optional<Long> result = jwtService.validateAndGetStudentId(token);

        assertThat(result).isEmpty();
    }
}
