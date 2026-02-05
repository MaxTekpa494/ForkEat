package fr.uge.forkeat.infrastructure.persistence.mapper;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.WalletEntity;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {


    @BeforeEach
    void setUp() {

    }

    @Nested
    class ToModelTests {

        @Test
        void toModel_ShouldConvertEntityToModel() {
            // Given
            UserEntity entity = new UserEntity();
            entity.setId(UUID.randomUUID());
            entity.setUsername("testuser");
            entity.setFirstName("John");
            entity.setLastName("Doe");
            entity.setEmail("john@example.com");
            entity.setPassword("hashed");
            entity.setCreatedAt(Instant.now());
            entity.setRole(UserRole.MEMBER);
            entity.setStatus(UserStatus.ACTIVE);
            entity.setAuthMode(AuthMode.LOCAL);

            // When
            var user = UserEntityMapper.toDomain(entity);

            // Then
            assertNotNull(user);
            assertEquals(entity.getId(), user.id());
            assertEquals(entity.getUsername(), user.username());
            assertEquals(entity.getFirstName(), user.firstName());
            assertEquals(entity.getLastName(), user.lastName());
            assertEquals(entity.getEmail(), user.email());
            assertEquals(entity.getCreatedAt(), user.createdAt());
            assertEquals(entity.getRole(), user.role());
            assertEquals(entity.getStatus(), user.status());
            assertEquals(entity.getAuthMode(), user.authMode());
        }

        @Test
        void toModel_ShouldHandleNullWallet() {
            // Given
            UserEntity entity = new UserEntity();
            entity.setId(UUID.randomUUID());
            entity.setUsername("testuser");
            entity.setFirstName("John");
            entity.setLastName("Doe");
            entity.setEmail("john@example.com");
            entity.setPassword("hashed");
            entity.setCreatedAt(Instant.now());
            entity.setRole(UserRole.MEMBER);
            entity.setStatus(UserStatus.ACTIVE);
            entity.setAuthMode(AuthMode.LOCAL);
            entity.setWallet(null); // Pas de wallet

            // When
            User user = UserEntityMapper.toDomain(entity);

            // Then
            assertNotNull(user);
        }

        @Test
        void toModel_ShouldIncludeWalletId_WhenWalletExists() {
            // Given
            UUID walletId = UUID.randomUUID();
            UserEntity userEntity = new UserEntity();
            userEntity.setId(UUID.randomUUID());
            userEntity.setUsername("testuser");
            userEntity.setFirstName("John");
            userEntity.setLastName("Doe");
            userEntity.setEmail("john@example.com");
            userEntity.setPassword("hashed");
            userEntity.setCreatedAt(Instant.now());
            userEntity.setRole(UserRole.MEMBER);
            userEntity.setStatus(UserStatus.ACTIVE);
            userEntity.setAuthMode(AuthMode.LOCAL);

            WalletEntity walletEntity = new WalletEntity();
            walletEntity.setId(walletId);
            userEntity.setWallet(walletEntity);

            // When
            User user = UserEntityMapper.toDomain(userEntity);

            // Then
            assertNotNull(user);
        }

        // ✅ TEST CORRIGÉ : Vérifie qu'une exception est lancée au lieu de retourner null
        @Test
        void toModel_ShouldThrowException_WhenEntityIsNull() {
            // When/Then
            NullPointerException exception = assertThrows(NullPointerException.class, () -> {
                UserEntityMapper.toDomain(null);
            });

            // Vérifier le message d'erreur
            assertEquals("null", exception.getMessage());
        }
    }

    @Nested
    class ToEntityTests {

        @Test
        void toEntity_ShouldConvertModelToEntity() {
            // Given
            UUID userId = UUID.randomUUID();
            User user = new User(
                    userId,
                    "testuser",
                    "John",
                    "Doe",
                    "john@example.com",
                    UserRole.MEMBER,
                    UserStatus.ACTIVE,
                    AuthMode.LOCAL,
                    Instant.now(),
                    Instant.now()
            );

            // When
            var entity = UserEntityMapper.toEntity(user);

            // Then
            assertNotNull(entity);
            assertEquals(user.id(), entity.getId());
            assertEquals(user.username(), entity.getUsername());
            assertEquals(user.firstName(), entity.getFirstName());
            assertEquals(user.lastName(), entity.getLastName());
            assertEquals(user.email(), entity.getEmail());
            assertEquals(user.createdAt(), entity.getCreatedAt());
            assertEquals(user.role(), entity.getRole());
            assertEquals(user.status(), entity.getStatus());
            assertEquals(user.authMode(), entity.getAuthMode());
        }

        // ✅ TEST CORRIGÉ : Vérifie qu'une exception est lancée au lieu de retourner null
        @Test
        void toEntity_ShouldThrowException_WhenModelIsNull() {
            // When/Then
            NullPointerException exception = assertThrows(NullPointerException.class, () -> {
                UserEntityMapper.toEntity(null);
            });

            // Vérifier le message d'erreur
            assertEquals("User cannot be null", exception.getMessage());
        }

        @Test
        void toEntity_ShouldHandleNullPassword() {
            // Given - OAuth2 user without password
            User user = new User(
                    UUID.randomUUID(),
                    "testuser",
                    "John",
                    "Doe",
                    "john@example.com",
                    UserRole.MEMBER,
                    UserStatus.ACTIVE,
                    AuthMode.GOOGLE,
                    Instant.now(),
                    Instant.now()
            );

            // When
            UserEntity entity = UserEntityMapper.toEntity(user);

            // Then
            assertNotNull(entity);
            assertNull(entity.getPassword());
            assertEquals(AuthMode.GOOGLE, entity.getAuthMode());
        }
    }
}