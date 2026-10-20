package com.user.security;

import com.user.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
                "test-secret-test-secret-test-secret-test-secret-123456",
                60_000L);
    }

    @Test
    void generateToken_canBeValidatedAndParsed() {
        String token = tokenProvider.generateToken("42", Role.ADMIN);

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUserId(token)).isEqualTo("42");
        assertThat(tokenProvider.getRole(token)).isEqualTo(Role.ADMIN);
    }

    @Test
    void validateToken_returnsFalseForGarbage() {
        assertThat(tokenProvider.validateToken("not-a-valid-token")).isFalse();
    }

    @Test
    void validateToken_returnsFalseForExpiredToken() throws InterruptedException {
        JwtTokenProvider shortLived = new JwtTokenProvider(
                "test-secret-test-secret-test-secret-test-secret-123456", 1L);
        String token = shortLived.generateToken("1", Role.USER);
        Thread.sleep(10);

        assertThat(shortLived.validateToken(token)).isFalse();
    }
}
