package fr.uge.forkeat.service.model.user;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VerificationTokenTest {

    private VerificationToken createToken(Instant expiresAt) {
        return new VerificationToken(
                UUID.randomUUID(), UUID.randomUUID(), "token-value",
                VerificationTokenType.EMAIL_CONFIRMATION, null, null,
                expiresAt, Instant.now()
        );
    }

    @Nested
    class ConstructorTests {

        @Test
        void shouldCreateWithValidArguments() {
            var token = createToken(Instant.now().plus(1, ChronoUnit.HOURS));
            assertNotNull(token);
            assertEquals("token-value", token.token());
            assertEquals(VerificationTokenType.EMAIL_CONFIRMATION, token.type());
        }

        @Test
        void shouldAllowNullNewEmailAndPasswordHash() {
            var token = createToken(Instant.now().plus(1, ChronoUnit.HOURS));
            assertNull(token.newEmail());
            assertNull(token.passwordHash());
        }

        @Test
        void shouldCreateWithPasswordHash() {
            var token = new VerificationToken(
                    UUID.randomUUID(), UUID.randomUUID(), "code",
                    VerificationTokenType.PASSWORD_CHANGE, null, "hashed-password",
                    Instant.now().plus(10, ChronoUnit.MINUTES), Instant.now()
            );
            assertEquals("hashed-password", token.passwordHash());
            assertNull(token.newEmail());
        }

        @Test
        void shouldCreateWithNewEmail() {
            var token = new VerificationToken(
                    UUID.randomUUID(), UUID.randomUUID(), "code",
                    VerificationTokenType.EMAIL_CHANGE, "new@test.com", null,
                    Instant.now().plus(10, ChronoUnit.MINUTES), Instant.now()
            );
            assertEquals("new@test.com", token.newEmail());
            assertNull(token.passwordHash());
        }

        @Test
        void shouldThrowOnNullId() {
            assertThrows(NullPointerException.class, () -> new VerificationToken(
                    null, UUID.randomUUID(), "token",
                    VerificationTokenType.EMAIL_CONFIRMATION, null, null,
                    Instant.now(), Instant.now()
            ));
        }

        @Test
        void shouldThrowOnNullUserId() {
            assertThrows(NullPointerException.class, () -> new VerificationToken(
                    UUID.randomUUID(), null, "token",
                    VerificationTokenType.EMAIL_CONFIRMATION, null, null,
                    Instant.now(), Instant.now()
            ));
        }

        @Test
        void shouldThrowOnNullToken() {
            assertThrows(NullPointerException.class, () -> new VerificationToken(
                    UUID.randomUUID(), UUID.randomUUID(), null,
                    VerificationTokenType.EMAIL_CONFIRMATION, null, null,
                    Instant.now(), Instant.now()
            ));
        }

        @Test
        void shouldThrowOnNullType() {
            assertThrows(NullPointerException.class, () -> new VerificationToken(
                    UUID.randomUUID(), UUID.randomUUID(), "token",
                    null, null, null,
                    Instant.now(), Instant.now()
            ));
        }

        @Test
        void shouldThrowOnNullExpiresAt() {
            assertThrows(NullPointerException.class, () -> new VerificationToken(
                    UUID.randomUUID(), UUID.randomUUID(), "token",
                    VerificationTokenType.EMAIL_CONFIRMATION, null, null,
                    null, Instant.now()
            ));
        }
    }

    @Nested
    class IsExpiredTests {

        @Test
        void shouldReturnTrueWhenExpired() {
            var token = createToken(Instant.now().minus(1, ChronoUnit.HOURS));
            assertTrue(token.isExpired());
        }

        @Test
        void shouldReturnFalseWhenNotExpired() {
            var token = createToken(Instant.now().plus(1, ChronoUnit.HOURS));
            assertFalse(token.isExpired());
        }
    }
}
