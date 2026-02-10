package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.mapper.UserEntityMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPersistenceAdapterTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserPersistenceAdapter adapter;

    private User createTestUser(UUID id, String username, String email) {
        return new User(
                id,
                username,
                "John",
                "Doe",
                email,
                UserRole.MEMBER,
                UserStatus.ACTIVE,
                AuthMode.LOCAL,
                Instant.now(),
                Instant.now(),
                false
        );
    }

    private UserEntity createTestUserEntity(UUID id, String username, String email) {
        var entity = new UserEntity();
        entity.setId(id);
        entity.setUsername(username);
        entity.setFirstName("John");
        entity.setLastName("Doe");
        entity.setEmail(email);
        entity.setPassword("hashedPassword");
        entity.setRole(UserRole.MEMBER);
        entity.setStatus(UserStatus.ACTIVE);
        entity.setAuthMode(AuthMode.LOCAL);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    @Test
    void saveUser_ShouldSaveAndReturnUser() {
        // Given
        var userId = UUID.randomUUID();
        var userDomain = createTestUser(userId, "testuser", "test@example.com");
        var savedEntity = createTestUserEntity(userId, "testuser", "test@example.com");

        when(userRepository.save(any(UserEntity.class))).thenReturn(savedEntity);

        // When
        User result = adapter.saveUser(userDomain, "hashedPassword");

        // Then
        assertNotNull(result);
        assertEquals(userId, result.id());
        assertEquals("testuser", result.username());
        assertEquals("test@example.com", result.email());
        assertEquals("John", result.firstName());
        assertEquals("Doe", result.lastName());
        assertEquals(UserRole.MEMBER, result.role());
        assertEquals(UserStatus.ACTIVE, result.status());
        assertEquals(AuthMode.LOCAL, result.authMode());

        verify(userRepository).save(argThat(entity ->
                entity.getUsername().equals("testuser") &&
                        entity.getEmail().equals("test@example.com") &&
                        entity.getPassword().equals("hashedPassword") &&
                        entity.getFirstName().equals("John") &&
                        entity.getLastName().equals("Doe") &&
                        entity.getRole() == UserRole.MEMBER &&
                        entity.getStatus() == UserStatus.ACTIVE &&
                        entity.getAuthMode() == AuthMode.LOCAL
        ));
    }

    @Test
    void findById_ShouldReturnUser_WhenExists() {
        // Given
        var userId = UUID.randomUUID();
        var userEntity = createTestUserEntity(userId, "testuser", "test@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

        // When
        Optional<User> result = adapter.findById(userId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(userId, result.get().id());
        assertEquals("testuser", result.get().username());
        assertEquals("test@example.com", result.get().email());
        verify(userRepository).findById(userId);
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        // Given
        var unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        // When
        Optional<User> result = adapter.findById(unknownId);

        // Then
        assertTrue(result.isEmpty());
        verify(userRepository).findById(unknownId);
    }

    @Test
    void findByEmail_ShouldReturnUser_WhenExists() {
        // Given
        var email = "test@example.com";
        var userId = UUID.randomUUID();
        var userEntity = createTestUserEntity(userId, "testuser", email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(userEntity));

        // When
        Optional<User> result = adapter.findByEmail(email);

        // Then
        assertTrue(result.isPresent());
        assertEquals(email, result.get().email());
        assertEquals("testuser", result.get().username());
        verify(userRepository).findByEmail(email);
    }

    @Test
    void findByEmail_ShouldReturnEmpty_WhenNotExists() {
        // Given
        var unknownEmail = "unknown@example.com";
        when(userRepository.findByEmail(unknownEmail)).thenReturn(Optional.empty());

        // When
        Optional<User> result = adapter.findByEmail(unknownEmail);

        // Then
        assertTrue(result.isEmpty());
        verify(userRepository).findByEmail(unknownEmail);
    }

    @Test
    void findByUsername_ShouldReturnUser_WhenExists() {
        // Given
        var username = "testuser";
        var userId = UUID.randomUUID();
        var userEntity = createTestUserEntity(userId, username, "test@example.com");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(userEntity));

        // When
        Optional<User> result = adapter.findByUsername(username);

        // Then
        assertTrue(result.isPresent());
        assertEquals(username, result.get().username());
        assertEquals("test@example.com", result.get().email());
        verify(userRepository).findByUsername(username);
    }

    @Test
    void findByUsername_ShouldReturnEmpty_WhenNotExists() {
        // Given
        var unknownUsername = "unknownuser";
        when(userRepository.findByUsername(unknownUsername)).thenReturn(Optional.empty());

        // When
        Optional<User> result = adapter.findByUsername(unknownUsername);

        // Then
        assertTrue(result.isEmpty());
        verify(userRepository).findByUsername(unknownUsername);
    }

    @Test
    void existsByEmail_ShouldReturnTrue_WhenExists() {
        // Given
        var email = "existing@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // When
        boolean result = adapter.existsByEmail(email);

        // Then
        assertTrue(result);
        verify(userRepository).existsByEmail(email);
    }

    @Test
    void existsByEmail_ShouldReturnFalse_WhenNotExists() {
        // Given
        var email = "nonexistent@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(false);

        // When
        boolean result = adapter.existsByEmail(email);

        // Then
        assertFalse(result);
        verify(userRepository).existsByEmail(email);
    }

    @Test
    void existsByUsername_ShouldReturnTrue_WhenExists() {
        // Given
        var username = "existinguser";
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // When
        boolean result = adapter.existsByUsername(username);

        // Then
        assertTrue(result);
        verify(userRepository).existsByUsername(username);
    }

    @Test
    void existsByUsername_ShouldReturnFalse_WhenNotExists() {
        // Given
        var username = "nonexistentuser";
        when(userRepository.existsByUsername(username)).thenReturn(false);

        // When
        boolean result = adapter.existsByUsername(username);

        // Then
        assertFalse(result);
        verify(userRepository).existsByUsername(username);
    }
}