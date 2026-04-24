package com.stockpulse.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "stockpulse-dev-secret-key-minimum-256-bits-long";
    private static final long EXPIRATION = 86_400_000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION);
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtUtil.generateToken("user-1");
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void generateToken_producesValidToken() {
        String token = jwtUtil.generateToken("user-1");
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void extractUserId_returnsCorrectSubject() {
        String token = jwtUtil.generateToken("user-42");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo("user-42");
    }

    @Test
    void validateToken_withInvalidSignature_returnsFalse() {
        String token = jwtUtil.generateToken("user-1");
        // tamper the signature (last segment)
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";
        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_withRandomString_returnsFalse() {
        assertThat(jwtUtil.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    void validateToken_withBlankToken_returnsFalse() {
        assertThat(jwtUtil.validateToken("")).isFalse();
    }

    @Test
    void validateToken_withExpiredToken_returnsFalse() {
        JwtUtil shortLivedUtil = new JwtUtil(SECRET, -1000L);
        String expiredToken = shortLivedUtil.generateToken("user-1");
        assertThat(jwtUtil.validateToken(expiredToken)).isFalse();
    }

    @Test
    void generateToken_differentUsersProduceDifferentTokens() {
        String t1 = jwtUtil.generateToken("user-1");
        String t2 = jwtUtil.generateToken("user-2");
        assertThat(t1).isNotEqualTo(t2);
    }
}
