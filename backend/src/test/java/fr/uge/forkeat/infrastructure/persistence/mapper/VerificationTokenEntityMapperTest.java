package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.VerificationTokenEntity;
import fr.uge.forkeat.service.model.user.VerificationToken;
import fr.uge.forkeat.service.model.user.VerificationTokenType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VerificationTokenEntityMapperTest {

    @Nested
    class ToDomainTests {

        @Test
        void shouldMapAllFields() {
            var entity = new VerificationTokenEntity();
            var id = UUID.randomUUID();
            var userId = UUID.randomUUID();
            var expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);

            entity.setId(id);
            entity.setUserId(userId);
            entity.setToken("test-token");
            entity.setType(VerificationTokenType.EMAIL_CHANGE);
            entity.setNewEmail("new@test.com");
            entity.setExpiresAt(expiresAt);

            var domain = VerificationTokenEntityMapper.toDomain(entity);

            assertEquals(id, domain.id());
            assertEquals(userId, domain.userId());
            assertEquals("test-token", domain.token());
            assertEquals(VerificationTokenType.EMAIL_CHANGE, domain.type());
            assertEquals("new@test.com", domain.newEmail());
            assertEquals(expiresAt, domain.expiresAt());
        }

        @Test
        void shouldThrowOnNull() {
            assertThrows(NullPointerException.class, () -> VerificationTokenEntityMapper.toDomain(null));
        }
    }

    @Nested
    class ToEntityTests {

        @Test
        void shouldMapAllFields() {
            var id = UUID.randomUUID();
            var userId = UUID.randomUUID();
            var expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES);
            var createdAt = Instant.now();

            var domain = new VerificationToken(id, userId, "code-123456",
                    VerificationTokenType.PASSWORD_CHANGE, null, null,
                    expiresAt, createdAt);

            var entity = VerificationTokenEntityMapper.toEntity(domain);

            assertEquals(id, entity.getId());
            assertEquals(userId, entity.getUserId());
            assertEquals("code-123456", entity.getToken());
            assertEquals(VerificationTokenType.PASSWORD_CHANGE, entity.getType());
            assertNull(entity.getNewEmail());
            assertEquals(expiresAt, entity.getExpiresAt());
        }

        @Test
        void shouldThrowOnNull() {
            assertThrows(NullPointerException.class, () -> VerificationTokenEntityMapper.toEntity(null));
        }
    }

    @Test
    void shouldRoundTrip() {
        var id = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
        var createdAt = Instant.now();

        var original = new VerificationToken(id, userId, "round-trip-token",
                VerificationTokenType.EMAIL_CHANGE, "new@email.com", null,
                expiresAt, createdAt);

        var entity = VerificationTokenEntityMapper.toEntity(original);
        var result = VerificationTokenEntityMapper.toDomain(entity);

        assertEquals(original.id(), result.id());
        assertEquals(original.userId(), result.userId());
        assertEquals(original.token(), result.token());
        assertEquals(original.type(), result.type());
        assertEquals(original.newEmail(), result.newEmail());
        assertEquals(original.expiresAt(), result.expiresAt());
    }
}
