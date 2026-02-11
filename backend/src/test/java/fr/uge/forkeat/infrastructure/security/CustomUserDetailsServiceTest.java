package fr.uge.forkeat.infrastructure.security;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new CustomUserDetailsService(userRepository);
    }

    private UserEntity createUserEntity(UserRole role, boolean emailVerified, String password) {
        var entity = new UserEntity("testuser", "Test", "User", password, "test@forkeat.fr",
                role, UserStatus.ACTIVE, AuthMode.LOCAL);
        entity.setId(UUID.randomUUID());
        entity.setEmailVerified(emailVerified);
        return entity;
    }

    @Nested
    class LoadByUsernameTests {

        @Test
        void shouldLoadByUsername() {
            var entity = createUserEntity(UserRole.MEMBER, false, "hashed-password");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(entity));

            var userDetails = service.loadUserByUsername("testuser");

            assertEquals("testuser", userDetails.getUsername());
            assertEquals("hashed-password", userDetails.getPassword());
        }

        @Test
        void shouldFallbackToEmail() {
            var entity = createUserEntity(UserRole.MEMBER, false, "hashed-password");
            when(userRepository.findByUsername("test@forkeat.fr")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("test@forkeat.fr")).thenReturn(Optional.of(entity));

            var userDetails = service.loadUserByUsername("test@forkeat.fr");

            assertEquals("testuser", userDetails.getUsername());
        }

        @Test
        void shouldThrowWhenNotFound() {
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("unknown"));
        }
    }

    @Nested
    class AuthorityTests {

        @Test
        void shouldHaveRoleMemberAuthority() {
            var entity = createUserEntity(UserRole.MEMBER, false, "pw");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(entity));

            var userDetails = service.loadUserByUsername("testuser");

            assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MEMBER")));
        }

        @Test
        void shouldHaveRoleAdminAuthority() {
            var entity = createUserEntity(UserRole.ADMIN, false, "pw");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(entity));

            var userDetails = service.loadUserByUsername("testuser");

            assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        }

        @Test
        void shouldHaveEmailVerifiedAuthority() {
            var entity = createUserEntity(UserRole.MEMBER, true, "pw");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(entity));

            var userDetails = service.loadUserByUsername("testuser");

            assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("EMAIL_VERIFIED")));
        }

        @Test
        void shouldNotHaveEmailVerifiedAuthorityWhenNotVerified() {
            var entity = createUserEntity(UserRole.MEMBER, false, "pw");
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(entity));

            var userDetails = service.loadUserByUsername("testuser");

            assertFalse(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("EMAIL_VERIFIED")));
        }
    }

    @Nested
    class OAuthUserTests {

        @Test
        void shouldUseEmptyPasswordForGoogleUser() {
            var entity = createUserEntity(UserRole.MEMBER, true, null);
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(entity));

            var userDetails = service.loadUserByUsername("testuser");

            assertEquals("", userDetails.getPassword());
        }
    }
}
