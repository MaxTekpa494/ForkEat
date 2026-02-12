package fr.uge.forkeat.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        // Secret doit faire au moins 32 bytes pour HS256
        ReflectionTestUtils.setField(jwtUtils, "secret", "test-secret-key-that-is-long-enough-for-hmac-sha256");
        ReflectionTestUtils.setField(jwtUtils, "expirationTime", 600000L); // 10 minutes
    }

    @Nested
    class GenerateTokenTests {

        @Test
        void shouldGenerateNonNullToken() {
            var token = jwtUtils.generateToken("testuser");
            assertNotNull(token);
            assertFalse(token.isEmpty());
        }

        @Test
        void shouldGenerateDifferentTokensForDifferentUsers() {
            var token1 = jwtUtils.generateToken("user1");
            var token2 = jwtUtils.generateToken("user2");
            assertNotEquals(token1, token2);
        }
    }

    @Nested
    class ExtractUsernameTests {

        @Test
        void shouldExtractCorrectUsername() {
            var token = jwtUtils.generateToken("chef_arnaud");
            var username = jwtUtils.extractUsername(token);
            assertEquals("chef_arnaud", username);
        }
    }

    @Nested
    class ValidateTokenTests {

        @Test
        void shouldReturnTrueForValidToken() {
            var token = jwtUtils.generateToken("testuser");
            UserDetails userDetails = User.withUsername("testuser")
                    .password("password")
                    .authorities(Collections.emptyList())
                    .build();

            assertTrue(jwtUtils.validateToken(token, userDetails));
        }

        @Test
        void shouldReturnFalseForWrongUsername() {
            var token = jwtUtils.generateToken("testuser");
            UserDetails userDetails = User.withUsername("otheruser")
                    .password("password")
                    .authorities(Collections.emptyList())
                    .build();

            assertFalse(jwtUtils.validateToken(token, userDetails));
        }

        @Test
        void shouldThrowForExpiredToken() {
            // Set negative expiration so token is immediately expired
            ReflectionTestUtils.setField(jwtUtils, "expirationTime", -1000L);
            var token = jwtUtils.generateToken("testuser");

            UserDetails userDetails = User.withUsername("testuser")
                    .password("password")
                    .authorities(Collections.emptyList())
                    .build();

            assertThrows(io.jsonwebtoken.ExpiredJwtException.class,
                    () -> jwtUtils.validateToken(token, userDetails));
        }
    }
}
