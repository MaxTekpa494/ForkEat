package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.model.user.Wallet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WalletEntityMapperTest {

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
            var walletId = UUID.randomUUID();
            var updatedAt = Instant.now();
            var entity = new WalletEntity(1000L, user);
            entity.setId(walletId);
            entity.setUpdatedAt(updatedAt);

            // When
            var wallet = WalletEntityMapper.toDomain(entity);

            // Then
            assertNotNull(wallet);
            assertEquals(walletId, wallet.id());
            assertEquals(1000L, wallet.balance());
            assertEquals(user.getId(), wallet.userId());
            assertEquals(updatedAt, wallet.updatedAt());
        }

        @Test
        void toDomain_ShouldThrowException_WhenUserIsNull() {
            // Given
            var entity = new WalletEntity();
            entity.setId(UUID.randomUUID());
            entity.setBalance(500L);
            entity.setUpdatedAt(Instant.now());

            // When/Then
            assertThrows(NullPointerException.class, () -> {
                WalletEntityMapper.toDomain(entity);
            });
        }

        @Test
        void toDomain_ShouldThrowException_WhenEntityIsNull() {
            // When/Then
            NullPointerException exception = assertThrows(NullPointerException.class, () -> {
                WalletEntityMapper.toDomain(null);
            });

            // Then
            assertEquals("WalletEntity cannot be null", exception.getMessage());
        }
    }

    @Nested
    class EntityBehaviorTests {

        @Test
        void walletEntity_ShouldMaintainBalance() {
            // Given
            var user = createUserEntity();
            var entity = new WalletEntity(1000L, user);
            entity.setId(UUID.randomUUID());

            // When
            entity.setBalance(2000L);

            // Then
            assertEquals(2000L, entity.getBalance());
        }

        @Test
        void walletEntity_ShouldMaintainTimestamp() {
            // Given
            var user = createUserEntity();
            var entity = new WalletEntity(1000L, user);
            entity.setId(UUID.randomUUID());

            var newTimestamp = Instant.now().plusSeconds(60);

            // When
            entity.setUpdatedAt(newTimestamp);

            // Then
            assertEquals(newTimestamp, entity.getUpdatedAt());
        }

        @Test
        void walletEntity_ShouldMaintainId() {
            // Given
            var user = createUserEntity();
            var originalId = UUID.randomUUID();
            var entity = new WalletEntity(1000L, user);

            // When
            entity.setId(originalId);

            // Then
            assertEquals(originalId, entity.getId());
        }

        @Test
        void walletEntity_ShouldMaintainUserReference() {
            // Given
            var user = createUserEntity();
            var entity = new WalletEntity(1000L, user);
            entity.setId(UUID.randomUUID());

            // When
            entity.setBalance(2000L);

            // Then - La référence utilisateur ne doit pas changer
            assertEquals(user, entity.getUser());
            assertEquals(user.getId(), entity.getUser().getId());
        }

        @Test
        void walletEntity_ShouldAllowBidirectionalRelationship() {
            // Given
            var user = createUserEntity();
            var walletEntity = new WalletEntity(1000L, user);
            walletEntity.setId(UUID.randomUUID());

            // When
            user.setWallet(walletEntity);

            // Then
            assertEquals(walletEntity, user.getWallet());
            assertEquals(user, walletEntity.getUser());
        }
    }
}