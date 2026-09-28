package fr.uge.forkeat.infrastructure.security;

import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.persistence.UserPersistence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserPersistence userPersistence;

    private CustomUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new CustomUserDetailsService(userPersistence);
    }

    private User createUser(UserRole role, boolean emailVerified) {
        return new User(
                UUID.randomUUID(),
                "testuser",
                "Test",
                "User",
                "test@forkeat.fr",
                role,
                UserStatus.ACTIVE,
                AuthMode.LOCAL,
                Instant.now(),
                Instant.now(),
                emailVerified
        );
    }

    @Nested
    class LoadByUsernameTests {

        @Test
        void shouldLoadByUsername() {
            var user = createUser(UserRole.MEMBER, false);
            when(userPersistence.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(userPersistence.findPasswordHashByUsername("testuser")).thenReturn("hashed-password");

            var userDetails = service.loadUserByUsername("testuser");

            assertEquals("testuser", userDetails.getUsername());
            assertEquals("hashed-password", userDetails.getPassword());
        }

        @Test
        void shouldFallbackToEmail() {
            var user = createUser(UserRole.MEMBER, false);
            when(userPersistence.findByUsername("test@forkeat.fr")).thenReturn(Optional.empty());
            when(userPersistence.findByEmail("test@forkeat.fr")).thenReturn(Optional.of(user));
            when(userPersistence.findPasswordHashByUsername("testuser")).thenReturn("hashed-password");

            var userDetails = service.loadUserByUsername("test@forkeat.fr");

            assertEquals("testuser", userDetails.getUsername());
        }

        @Test
        void shouldThrowWhenNotFound() {
            when(userPersistence.findByUsername("unknown")).thenReturn(Optional.empty());
            when(userPersistence.findByEmail("unknown")).thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("unknown"));
        }
    }

    @Nested
    class AuthorityTests {

        @Test
        void shouldHaveRoleMemberAuthority() {
            var user = createUser(UserRole.MEMBER, false);
            when(userPersistence.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(userPersistence.findPasswordHashByUsername("testuser")).thenReturn("pw");

            var userDetails = service.loadUserByUsername("testuser");

            assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MEMBER")));
        }

        @Test
        void shouldHaveRoleAdminAuthority() {
            var user = createUser(UserRole.ADMIN, false);
            when(userPersistence.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(userPersistence.findPasswordHashByUsername("testuser")).thenReturn("pw");

            var userDetails = service.loadUserByUsername("testuser");

            assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        }

        @Test
        void shouldHaveEmailVerifiedAuthority() {
            var user = createUser(UserRole.MEMBER, true);
            when(userPersistence.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(userPersistence.findPasswordHashByUsername("testuser")).thenReturn("pw");

            var userDetails = service.loadUserByUsername("testuser");

            assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("EMAIL_VERIFIED")));
        }

        @Test
        void shouldNotHaveEmailVerifiedAuthorityWhenNotVerified() {
            var user = createUser(UserRole.MEMBER, false);
            when(userPersistence.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(userPersistence.findPasswordHashByUsername("testuser")).thenReturn("pw");

            var userDetails = service.loadUserByUsername("testuser");

            assertFalse(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("EMAIL_VERIFIED")));
        }
    }

    @Nested
    class OAuthUserTests {

        @Test
        void shouldUseEmptyPasswordForGoogleUser() {
            var user = createUser(UserRole.MEMBER, true);
            when(userPersistence.findByUsername("testuser")).thenReturn(Optional.of(user));
            when(userPersistence.findPasswordHashByUsername("testuser")).thenReturn(null);

            var userDetails = service.loadUserByUsername("testuser");

            assertEquals("", userDetails.getPassword());
        }
    }
}