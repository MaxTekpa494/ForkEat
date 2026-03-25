package fr.uge.forkeat.infrastructure.persistence.adapter;

import fr.uge.forkeat.infrastructure.persistence.neo4j.projection.UserSocialCountsProjection;
import fr.uge.forkeat.infrastructure.persistence.neo4j.repository.Neo4jUserRepository;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.projection.UserProfileView;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.Nested;
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

    @Mock
    private Neo4jUserRepository neo4jUserRepository;

    @InjectMocks
    private UserPersistenceAdapter adapter;

    private User createTestUser(UUID id) {
        return new User(
                id,
                "testuser",
                "John",
                "Doe",
                "test@example.com",
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
        var userDomain = createTestUser(userId);
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
    void existsById_ShouldReturnTrue_WhenExists() {
        var userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(true);

        boolean result = adapter.existsById(userId);

        assertTrue(result);
        verify(userRepository).existsById(userId);
    }

    @Test
    void existsById_ShouldReturnFalse_WhenNotExists() {
        var userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(false);

        boolean result = adapter.existsById(userId);

        assertFalse(result);
        verify(userRepository).existsById(userId);
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

    @Nested
    class SocialStats {

        @Test
        void countFollowers_ShouldDelegateToNeo4j() {
            var userId = UUID.randomUUID();
            when(neo4jUserRepository.countFollowers(userId)).thenReturn(5L);

            long result = adapter.countFollowers(userId);

            assertEquals(5L, result);
            verify(neo4jUserRepository).countFollowers(userId);
        }

        @Test
        void countFollowers_ShouldThrowWhenIdIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.countFollowers(null));
        }

        @Test
        void countFollowing_ShouldDelegateToNeo4j() {
            var userId = UUID.randomUUID();
            when(neo4jUserRepository.countFollowing(userId)).thenReturn(3L);

            long result = adapter.countFollowing(userId);

            assertEquals(3L, result);
            verify(neo4jUserRepository).countFollowing(userId);
        }

        @Test
        void countFollowing_ShouldThrowWhenIdIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.countFollowing(null));
        }

        @Test
        void countTotalLikesReceived_ShouldDelegateToNeo4j() {
            var userId = UUID.randomUUID();
            when(neo4jUserRepository.countTotalLikesReceived(userId)).thenReturn(42L);

            long result = adapter.countTotalLikesReceived(userId);

            assertEquals(42L, result);
            verify(neo4jUserRepository).countTotalLikesReceived(userId);
        }

        @Test
        void countTotalLikesReceived_ShouldThrowWhenIdIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.countTotalLikesReceived(null));
        }

        @Test
        void countTotalSuperLikesReceived_ShouldDelegateToNeo4j() {
            var userId = UUID.randomUUID();
            when(neo4jUserRepository.countTotalSuperLikesReceived(userId)).thenReturn(7L);

            long result = adapter.countTotalSuperLikesReceived(userId);

            assertEquals(7L, result);
            verify(neo4jUserRepository).countTotalSuperLikesReceived(userId);
        }

        @Test
        void countTotalSuperLikesReceived_ShouldThrowWhenIdIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.countTotalSuperLikesReceived(null));
        }
    }

    @Nested
    class FindUserProfile {

        @Test
        void shouldReturnPublicProfile_WhenUserExists() {
            var userId = UUID.randomUUID();
            var profileView = mock(UserProfileView.class);
            when(profileView.getUsername()).thenReturn("testuser");
            when(profileView.getFirstName()).thenReturn("John");
            when(profileView.getLastName()).thenReturn("Doe");

            when(userRepository.findProfileById(userId)).thenReturn(Optional.of(profileView));

            var result = adapter.findPublicProfile(userId);

            assertNotNull(result);
            assertEquals("testuser", result.username());
            assertEquals("John", result.firstName());
            assertEquals("Doe", result.lastName());
            verify(userRepository).findProfileById(userId);
        }

        @Test
        void shouldReturnSocialStats_WhenUserExists() {
            var userId = UUID.randomUUID();
            var counts = new UserSocialCountsProjection(10L, 5L, 30L, 2L);

            when(neo4jUserRepository.findSocialCountsByUserId(userId)).thenReturn(counts);

            var result = adapter.findUserSocialStats(userId);

            assertNotNull(result);
            assertEquals(10L, result.followerCount());
            assertEquals(5L, result.followingCount());
            assertEquals(30L, result.totalLikeCount());
            assertEquals(2L, result.totalSuperLikeCount());
            verify(neo4jUserRepository).findSocialCountsByUserId(userId);
        }

        @Test
        void shouldReturnZeroStats_WhenNeo4jReturnsNull() {
            var userId = UUID.randomUUID();

            when(neo4jUserRepository.findSocialCountsByUserId(userId)).thenReturn(null);

            var result = adapter.findUserSocialStats(userId);

            assertEquals(0L, result.followerCount());
            assertEquals(0L, result.followingCount());
            assertEquals(0L, result.totalLikeCount());
            assertEquals(0L, result.totalSuperLikeCount());
        }

        @Test
        void shouldThrow_WhenUserNotFound_ForPublicProfile() {
            var userId = UUID.randomUUID();
            when(userRepository.findProfileById(userId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> adapter.findPublicProfile(userId));
        }

        @Test
        void shouldThrow_WhenIdIsNull_ForPublicProfile() {
            assertThrows(NullPointerException.class, () -> adapter.findPublicProfile(null));
        }

        @Test
        void shouldThrow_WhenIdIsNull_ForSocialStats() {
            assertThrows(NullPointerException.class, () -> adapter.findUserSocialStats(null));
        }
    }

    @Nested
    class DeleteByIdTests {

        @Test
        void deleteById_ShouldDelegateToRepository() {
            // Given
            var userId = UUID.randomUUID();

            // When
            adapter.deleteById(userId);

            // Then
            verify(userRepository).deleteById(userId);
        }

        @Test
        void deleteById_ShouldThrow_WhenIdIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.deleteById(null));
        }
    }

    @Nested
    class FollowTests {

        @Test
        void findIdByUsername_ShouldReturnId_WhenExists() {
            var userId = UUID.randomUUID();
            when(userRepository.findIdByUsername("alice")).thenReturn(Optional.of(userId));

            var result = adapter.findIdByUsername("alice");

            assertTrue(result.isPresent());
            assertEquals(userId, result.get());
            verify(userRepository).findIdByUsername("alice");
        }

        @Test
        void findIdByUsername_ShouldReturnEmpty_WhenNotExists() {
            when(userRepository.findIdByUsername("unknown")).thenReturn(Optional.empty());

            var result = adapter.findIdByUsername("unknown");

            assertTrue(result.isEmpty());
            verify(userRepository).findIdByUsername("unknown");
        }

        @Test
        void findIdByUsername_ShouldThrow_WhenNull() {
            assertThrows(NullPointerException.class, () -> adapter.findIdByUsername(null));
        }

        @Test
        void follow_ShouldDelegateToNeo4j() {
            var followerId = UUID.randomUUID();
            var followedId = UUID.randomUUID();

            adapter.follow(followerId, followedId);

            verify(neo4jUserRepository).follow(eq(followerId), eq(followedId), any(Instant.class));
        }

        @Test
        void follow_ShouldThrow_WhenFollowerIdIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.follow(null, UUID.randomUUID()));
        }

        @Test
        void follow_ShouldThrow_WhenFollowedIdIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.follow(UUID.randomUUID(), null));
        }

        @Test
        void unfollow_ShouldDelegateToNeo4j() {
            var followerId = UUID.randomUUID();
            var followedId = UUID.randomUUID();

            adapter.unfollow(followerId, followedId);

            verify(neo4jUserRepository).unfollow(followerId, followedId);
        }

        @Test
        void unfollow_ShouldThrow_WhenFollowerIdIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.unfollow(null, UUID.randomUUID()));
        }

        @Test
        void unfollow_ShouldThrow_WhenFollowedIdIsNull() {
            assertThrows(NullPointerException.class, () -> adapter.unfollow(UUID.randomUUID(), null));
        }
    }
}