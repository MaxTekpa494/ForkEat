package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.VerificationTokenEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.VerificationTokenRepository;
import fr.uge.forkeat.service.model.user.VerificationToken;
import fr.uge.forkeat.service.model.user.VerificationTokenType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationTokenPersistenceAdapterTest {

    @Mock
    private VerificationTokenRepository repository;

    @InjectMocks
    private VerificationTokenPersistenceAdapter adapter;

    private VerificationToken createDomainToken(UUID id, UUID userId, VerificationTokenType type) {
        return new VerificationToken(id, userId, "token-value", type, null,
                Instant.now().plus(24, ChronoUnit.HOURS), Instant.now());
    }

    private VerificationTokenEntity createEntity(UUID id, UUID userId) {
        var entity = new VerificationTokenEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setToken("token-value");
        entity.setType(VerificationTokenType.EMAIL_CONFIRMATION);
        entity.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        return entity;
    }

    @Nested
    class SaveTests {

        @Test
        void shouldSaveAndReturnDomainToken() {
            var id = UUID.randomUUID();
            var userId = UUID.randomUUID();
            var domain = createDomainToken(id, userId, VerificationTokenType.EMAIL_CONFIRMATION);
            var entity = createEntity(id, userId);

            when(repository.save(any(VerificationTokenEntity.class))).thenReturn(entity);

            var result = adapter.save(domain);

            assertNotNull(result);
            assertEquals(id, result.id());
            assertEquals(userId, result.userId());
            verify(repository).save(any(VerificationTokenEntity.class));
        }

        @Test
        void shouldThrowOnNullToken() {
            assertThrows(NullPointerException.class, () -> adapter.save(null));
        }
    }

    @Nested
    class FindByTokenTests {

        @Test
        void shouldReturnTokenWhenFound() {
            var id = UUID.randomUUID();
            var userId = UUID.randomUUID();
            var entity = createEntity(id, userId);

            when(repository.findByToken("token-value")).thenReturn(Optional.of(entity));

            var result = adapter.findByToken("token-value");

            assertTrue(result.isPresent());
            assertEquals(id, result.get().id());
        }

        @Test
        void shouldReturnEmptyWhenNotFound() {
            when(repository.findByToken("unknown")).thenReturn(Optional.empty());

            var result = adapter.findByToken("unknown");

            assertTrue(result.isEmpty());
        }

        @Test
        void shouldThrowOnNullToken() {
            assertThrows(NullPointerException.class, () -> adapter.findByToken(null));
        }
    }

    @Nested
    class FindByUserIdAndTypeTests {

        @Test
        void shouldReturnTokenWhenFound() {
            var id = UUID.randomUUID();
            var userId = UUID.randomUUID();
            var entity = createEntity(id, userId);

            when(repository.findByUserIdAndType(userId, VerificationTokenType.EMAIL_CONFIRMATION))
                    .thenReturn(Optional.of(entity));

            var result = adapter.findByUserIdAndType(userId, VerificationTokenType.EMAIL_CONFIRMATION);

            assertTrue(result.isPresent());
            assertEquals(userId, result.get().userId());
        }

        @Test
        void shouldThrowOnNullType() {
            assertThrows(NullPointerException.class,
                    () -> adapter.findByUserIdAndType(UUID.randomUUID(), null));
        }

        @Test
        void shouldThrowOnNullUserId() {
            assertThrows(NullPointerException.class,
                    () -> adapter.findByUserIdAndType(null, VerificationTokenType.EMAIL_CONFIRMATION));
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void shouldDelegateToRepository() {
            var userId = UUID.randomUUID();

            adapter.deleteByUserIdAndType(userId, VerificationTokenType.EMAIL_CONFIRMATION);

            verify(repository).deleteByUserIdAndType(userId, VerificationTokenType.EMAIL_CONFIRMATION);
        }

        @Test
        void shouldThrowOnNullType() {
            assertThrows(NullPointerException.class,
                    () -> adapter.deleteByUserIdAndType(UUID.randomUUID(), null));
        }

        @Test
        void shouldThrowOnNullUserId() {
            assertThrows(NullPointerException.class,
                    () -> adapter.deleteByUserIdAndType(null, VerificationTokenType.EMAIL_CONFIRMATION));
        }
    }
}
