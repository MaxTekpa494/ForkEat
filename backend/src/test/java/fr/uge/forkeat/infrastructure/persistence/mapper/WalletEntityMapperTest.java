package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.mapper.WalletMapper;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WalletMapperTest {

    private WalletMapper walletMapper;

    @BeforeEach
    void setUp() {
        walletMapper = new WalletMapper();
    }

    private UserEntity createUserEntity() {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("test@example.com");
        user.setPassword("hashed");
        user.setCreatedAt(Instant.now());
        user.setRole(UserRole.MEMBER);
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthMode(AuthMode.LOCAL);
        return user;
    }

    @Nested
    class ToDomainTests {

        @Test
        void toDomain_ShouldConvertEntityToDomain() {
            // Given
            var user = createUserEntity();
            var entity = new WalletEntity(1000L, user);
            entity.setId(UUID.randomUUID());
            entity.setUpdatedAt(Instant.now());

            // When
            var wallet = walletMapper.toDomain(entity);

            // Then
            assertNotNull(wallet);
            assertEquals(entity.getId(), wallet.id());
            assertEquals(entity.getBalance(), wallet.balance());
            assertEquals(user.getId(), wallet.userId());
            assertEquals(entity.getUpdatedAt(), wallet.updatedAt());
        }

        @Test
        void toDomain_ShouldHandleNullUser() {
            // Given
            var entity = new WalletEntity();
            entity.setId(UUID.randomUUID());
            entity.setBalance(500L);
            entity.setUpdatedAt(Instant.now());
            entity.setUser(null); // Pas d'utilisateur

            // When
            var wallet = walletMapper.toDomain(entity);

            // Then
            assertNotNull(wallet);
            assertNull(wallet.userId());
        }

        // ✅ TEST CORRIGÉ : Vérifie qu'une exception est lancée au lieu de retourner null
        @Test
        void toDomain_ShouldThrowException_WhenEntityIsNull() {
            // When/Then
            NullPointerException exception = assertThrows(NullPointerException.class, () -> {
                walletMapper.toDomain(null);
            });

            // Vérifier le message d'erreur
            assertEquals("WalletEntity cannot be null", exception.getMessage());
        }
    }

    @Nested
    class UpdateEntityTests {

        @Test
        void updateEntity_ShouldUpdateBalanceAndTimestamp() {
            // Given
            var user = createUserEntity();
            var entity = new WalletEntity(1000L, user);
            entity.setId(UUID.randomUUID());

            var newTimestamp = Instant.now().plusSeconds(60);
            var wallet = new Wallet(entity.getId(), 2000L, user.getId(), newTimestamp);

            // When
            walletMapper.updateEntity(entity, wallet);

            // Then
            assertEquals(2000L, entity.getBalance());
            assertEquals(newTimestamp, entity.getUpdatedAt());
        }

        @Test
        void updateEntity_ShouldNotChangeId() {
            // Given
            var user = createUserEntity();
            var originalId = UUID.randomUUID();
            var entity = new WalletEntity(1000L, user);
            entity.setId(originalId);

            var differentId = UUID.randomUUID();
            var wallet = new Wallet(differentId, 2000L, user.getId(), Instant.now());

            // When
            walletMapper.updateEntity(entity, wallet);

            // Then - ID ne doit pas changer
            assertEquals(originalId, entity.getId());
        }

        @Test
        void updateEntity_ShouldNotChangeUser() {
            // Given
            var user = createUserEntity();
            var entity = new WalletEntity(1000L, user);
            entity.setId(UUID.randomUUID());

            var differentUserId = UUID.randomUUID();
            var wallet = new Wallet(entity.getId(), 2000L, differentUserId, Instant.now());

            // When
            walletMapper.updateEntity(entity, wallet);

            // Then - User ne doit pas changer
            assertEquals(user, entity.getUser());
        }

        // ✅ TEST CORRIGÉ : Vérifie qu'une exception est lancée au lieu d'ignorer
        @Test
        void updateEntity_ShouldThrowException_WhenEntityIsNull() {
            // Given
            var wallet = new Wallet(UUID.randomUUID(), 1000L, UUID.randomUUID(), Instant.now());

            // When/Then
            NullPointerException exception = assertThrows(NullPointerException.class, () -> {
                walletMapper.updateEntity(null, wallet);
            });

            // Vérifier le message d'erreur
            assertEquals("WalletEntity cannot be null", exception.getMessage());
        }

        // ✅ TEST CORRIGÉ : Vérifie qu'une exception est lancée au lieu d'ignorer
        @Test
        void updateEntity_ShouldThrowException_WhenDomainIsNull() {
            // Given
            var user = createUserEntity();
            var entity = new WalletEntity(1000L, user);
            entity.setId(UUID.randomUUID());
            var originalBalance = entity.getBalance();
            var originalUpdatedAt = entity.getUpdatedAt();

            // When/Then
            NullPointerException exception = assertThrows(NullPointerException.class, () -> {
                walletMapper.updateEntity(entity, null);
            });

            // Vérifier le message d'erreur
            assertEquals("Wallet cannot be null", exception.getMessage());

            // Vérifier que l'entité n'a pas été modifiée avant l'exception
            assertEquals(originalBalance, entity.getBalance());
        }
    }
}