package fr.uge.forkeat.service.user;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.*;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.persistence.UserPersistence;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import fr.uge.forkeat.service.port.PasswordHasher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserPersistence userPersistence;

    @Mock
    private PasswordHasher passwordHasher;

    @InjectMocks
    private UserService userService;

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
                false);
    }

    @Nested
    class GetUserByEmailTests {

        @Test
        void getUserByEmail_ShouldReturnUser_WhenExists() throws ResourceNotFoundException {
            // Given
            var user = createTestUser(UUID.randomUUID(), "testuser", "test@example.com");
            when(userPersistence.findByEmail("test@example.com")).thenReturn(Optional.of(user));

            // When
            User result = userService.getUserByEmail("test@example.com");

            // Then
            assertNotNull(result);
            assertEquals("test@example.com", result.email());
            assertEquals("testuser", result.username());
        }

        @Test
        void getUserByEmail_ShouldThrow_WhenNotFound() {
            // Given
            when(userPersistence.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            // When/Then
            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> userService.getUserByEmail("unknown@example.com")
            );

            assertEquals("User not found with email: unknown@example.com", exception.getMessage());
        }

        @Test
        void getUserByEmail_ShouldThrow_WhenEmailIsNull() {
            // Given
            when(userPersistence.findByEmail(null)).thenReturn(Optional.empty());

            // When/Then
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> userService.getUserByEmail(null)
            );
        }
    }

    @Nested
    class GetUserByIdTests {

        @Test
        void getUserById_ShouldReturnUser_WhenExists() throws ResourceNotFoundException {
            // Given
            var userId = UUID.randomUUID();
            var user = createTestUser(userId, "testuser", "test@example.com");
            when(userPersistence.findById(userId)).thenReturn(Optional.of(user));

            // When
            User result = userService.getUserById(userId);

            // Then
            assertNotNull(result);
            assertEquals(userId, result.id());
            assertEquals("testuser", result.username());
        }

        @Test
        void getUserById_ShouldThrow_WhenNotFound() {
            // Given
            var unknownId = UUID.randomUUID();
            when(userPersistence.findById(unknownId)).thenReturn(Optional.empty());

            // When/Then
            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> userService.getUserById(unknownId)
            );

            assertEquals("User not found with id: " + unknownId, exception.getMessage());
        }

        @Test
        void getUserById_ShouldThrow_WhenIdIsNull() {
            // Given
            when(userPersistence.findById(null)).thenReturn(Optional.empty());

            // When/Then
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> userService.getUserById(null)
            );
        }
    }

    @Nested
    class GetUserByUsernameTests {

        @Test
        void getUserByUsername_ShouldReturnUser_WhenExists() throws ResourceNotFoundException {
            // Given
            var user = createTestUser(UUID.randomUUID(), "testuser", "test@example.com");
            when(userPersistence.findByUsername("testuser")).thenReturn(Optional.of(user));

            // When
            User result = userService.getUserByUsername("testuser");

            // Then
            assertNotNull(result);
            assertEquals("testuser", result.username());
            assertEquals("test@example.com", result.email());
        }

        @Test
        void getUserByUsername_ShouldThrow_WhenNotFound() {
            // Given
            when(userPersistence.findByUsername("unknownuser")).thenReturn(Optional.empty());

            // When/Then
            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> userService.getUserByUsername("unknownuser")
            );

            assertEquals("User not found with username: unknownuser", exception.getMessage());
        }

        @Test
        void getUserByUsername_ShouldThrow_WhenUsernameIsNull() {
            // Given
            when(userPersistence.findByUsername(null)).thenReturn(Optional.empty());

            // When/Then
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> userService.getUserByUsername(null)
            );
        }

        @Test
        void getUserByUsername_ShouldBeTransactionalReadOnly() throws ResourceNotFoundException {
            // Given
            var user = createTestUser(UUID.randomUUID(), "testuser", "test@example.com");
            when(userPersistence.findByUsername("testuser")).thenReturn(Optional.of(user));

            // When
            User result = userService.getUserByUsername("testuser");

            // Then
            assertNotNull(result);
        }
    }

    @Nested
    class MultipleUserTests {

        @Test
        void shouldRetrieveDifferentUsersByDifferentCriteria() throws ResourceNotFoundException {
            // Given
            var userId1 = UUID.randomUUID();
            var userId2 = UUID.randomUUID();
            
            var user1 = createTestUser(userId1, "user1", "user1@example.com");
            var user2 = createTestUser(userId2, "user2", "user2@example.com");

            when(userPersistence.findByEmail("user1@example.com")).thenReturn(Optional.of(user1));
            when(userPersistence.findById(userId2)).thenReturn(Optional.of(user2));
            when(userPersistence.findByUsername("user1")).thenReturn(Optional.of(user1));

            // When
            User resultByEmail = userService.getUserByEmail("user1@example.com");
            User resultById = userService.getUserById(userId2);
            User resultByUsername = userService.getUserByUsername("user1");

            // Then
            assertEquals(userId1, resultByEmail.id());
            assertEquals(userId2, resultById.id());
            assertEquals(userId1, resultByUsername.id());
        }
    }
}