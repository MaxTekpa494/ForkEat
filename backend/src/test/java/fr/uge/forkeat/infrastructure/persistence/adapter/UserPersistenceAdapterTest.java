package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.mapper.UserMapper;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserJpaRepository;
import fr.uge.forkeat.service.model.*;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPersistenceAdapterTest {

    @Mock
    private UserJpaRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserPersistenceAdapter adapter;

    private User createTestUser(UUID id, String username, String email) {
        return new User(
                id,
                username,
                "John",
                "Doe",
                email,
                "hashedPassword",
                Instant.now(),
                UserRole.MEMBER,
                UserStatus.ACTIVE,
                AuthMode.LOCAL,
                UUID.randomUUID()
        );
    }

    private UserEntity createTestUserEntity() {
        var entity = new UserEntity();
        entity.setId(UUID.randomUUID());
        entity.setUsername("testuser");
        entity.setFirstName("John");
        entity.setLastName("Doe");
        entity.setEmail("test@example.com");
        entity.setPassword("hashedPassword");
        entity.setRole(UserRole.MEMBER);
        entity.setStatus(UserStatus.ACTIVE);
        entity.setAuthMode(AuthMode.LOCAL);
        return entity;
    }

    @Test
    void saveUser_ShouldSaveAndReturnUser() {
        // Given
        var userId = UUID.randomUUID();
        var userDomain = createTestUser(userId, "testuser", "test@example.com");
        var userEntity = createTestUserEntity();

        when(userMapper.toEntity(userDomain)).thenReturn(userEntity);
        when(userRepository.save(userEntity)).thenReturn(userEntity);
        when(userMapper.toModel(userEntity)).thenReturn(userDomain);

        // When
        User result = adapter.saveUser(userDomain);

        // Then
        assertNotNull(result);
        assertEquals(userDomain, result);
        verify(userRepository).save(userEntity);
        verify(userMapper).toEntity(userDomain);
        verify(userMapper).toModel(userEntity);
    }

    @Test
    void findById_ShouldReturnUser_WhenExists() {
        // Given
        var userId = UUID.randomUUID();
        var userEntity = createTestUserEntity();
        var userDomain = createTestUser(userId, "testuser", "test@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userMapper.toModel(userEntity)).thenReturn(userDomain);

        // When
        Optional<User> result = adapter.findById(userId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(userDomain, result.get());
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
        verifyNoInteractions(userMapper);
    }

    @Test
    void findByEmail_ShouldReturnUser_WhenExists() {
        // Given
        var email = "test@example.com";
        var userEntity = createTestUserEntity();
        var userDomain = createTestUser(UUID.randomUUID(), "testuser", email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(userEntity));
        when(userMapper.toModel(userEntity)).thenReturn(userDomain);

        // When
        Optional<User> result = adapter.findByEmail(email);

        // Then
        assertTrue(result.isPresent());
        assertEquals(email, result.get().email());
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
    }

    @Test
    void findByUsername_ShouldReturnUser_WhenExists() {
        // Given
        var username = "testuser";
        var userEntity = createTestUserEntity();
        var userDomain = createTestUser(UUID.randomUUID(), username, "test@example.com");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(userEntity));
        when(userMapper.toModel(userEntity)).thenReturn(userDomain);

        // When
        Optional<User> result = adapter.findByUsername(username);

        // Then
        assertTrue(result.isPresent());
        assertEquals(username, result.get().username());
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
    }
}
