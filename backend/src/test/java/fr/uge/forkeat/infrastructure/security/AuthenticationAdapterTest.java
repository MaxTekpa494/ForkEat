package fr.uge.forkeat.infrastructure.security;

import fr.uge.forkeat.infrastructure.config.JwtUtils;
import fr.uge.forkeat.infrastructure.security.strategy.PrincipalExtractor;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationAdapterTest {

    private PrincipalExtractor extractor;
    private JwtUtils jwtUtils;
    private AuthenticationAdapter adapter;

    @BeforeEach
    void setUp() {
        extractor = mock(PrincipalExtractor.class);
        jwtUtils = mock(JwtUtils.class);
        adapter = new AuthenticationAdapter(List.of(extractor), jwtUtils);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User createUser(UserRole role, boolean emailVerified) {
        return new User(UUID.randomUUID(), "testuser", "Test", "User",
                "test@forkeat.fr", role, UserStatus.ACTIVE, AuthMode.LOCAL,
                Instant.now(), Instant.now(), emailVerified);
    }

    @Nested
    class ExtractUsernameTests {

        @Test
        void shouldExtractUsernameWithMatchingExtractor() {
            var auth = new UsernamePasswordAuthenticationToken("principal", null);
            SecurityContextHolder.getContext().setAuthentication(auth);

            when(extractor.supports("principal")).thenReturn(true);
            when(extractor.extractUsername("principal")).thenReturn("testuser");

            assertEquals("testuser", adapter.extractUsername());
        }

        @Test
        void shouldFallbackToAuthNameWhenNoExtractorMatches() {
            var auth = new UsernamePasswordAuthenticationToken("fallback-user", null);
            SecurityContextHolder.getContext().setAuthentication(auth);

            when(extractor.supports("fallback-user")).thenReturn(false);

            assertEquals("fallback-user", adapter.extractUsername());
        }

        @Test
        void shouldThrowWhenNoAuthentication() {
            SecurityContextHolder.clearContext();
            assertThrows(Exception.class, () -> adapter.extractUsername());
        }
    }

    @Nested
    class ExtractUserTests {

        @Test
        void shouldExtractUserWithMatchingExtractor() {
            var user = createUser(UserRole.MEMBER, true);
            var auth = new UsernamePasswordAuthenticationToken("principal", null);

            when(extractor.supports("principal")).thenReturn(true);
            when(extractor.extractUser("principal")).thenReturn(user);

            assertEquals(user, adapter.extractUser(auth));
        }

        @Test
        void shouldReturnNullWhenAuthenticationIsNull() {
            assertNull(adapter.extractUser(null));
        }
    }

    @Nested
    class IsOAuth2Tests {

        @Test
        void shouldReturnTrueForOAuth2() {
            var auth = new UsernamePasswordAuthenticationToken("principal", null);

            when(extractor.supports("principal")).thenReturn(true);
            when(extractor.isOAuth2("principal")).thenReturn(true);

            assertTrue(adapter.isOAuth2Authentication(auth));
        }

        @Test
        void shouldReturnFalseForNonOAuth2() {
            var auth = new UsernamePasswordAuthenticationToken("principal", null);

            when(extractor.supports("principal")).thenReturn(true);
            when(extractor.isOAuth2("principal")).thenReturn(false);

            assertFalse(adapter.isOAuth2Authentication(auth));
        }

        @Test
        void shouldReturnFalseForNullAuth() {
            assertFalse(adapter.isOAuth2Authentication(null));
        }
    }

    @Nested
    class GenerateTokenTests {

        @Test
        void shouldDelegateTokenGenerationToJwtUtils() {
            when(jwtUtils.generateToken("testuser")).thenReturn("mocked.jwt.token");

            var token = adapter.generateToken("testuser");

            assertEquals("mocked.jwt.token", token);
            verify(jwtUtils).generateToken("testuser");
        }
    }

    @Nested
    class IsAuthenticatedTests {

        @Test
        void shouldReturnTrueWhenUserIsAuthenticated() {
            var auth = new UsernamePasswordAuthenticationToken("testuser", null,
                    List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertTrue(adapter.isAuthenticated());
        }

        @Test
        void shouldReturnFalseWhenContextIsEmpty() {
            SecurityContextHolder.clearContext();

            assertFalse(adapter.isAuthenticated());
        }

        @Test
        void shouldReturnFalseWhenPrincipalIsAnonymousUser() {
            var auth = new UsernamePasswordAuthenticationToken("anonymousUser", null);
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertFalse(adapter.isAuthenticated());
        }
    }

    @Nested
    class IsAdminTests {

        @Test
        void shouldReturnTrueWhenUserHasAdminRole() {
            var auth = new UsernamePasswordAuthenticationToken("admin", null,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertTrue(adapter.isAdmin());
        }

        @Test
        void shouldReturnFalseWhenUserHasMemberRole() {
            var auth = new UsernamePasswordAuthenticationToken("user", null,
                    List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertFalse(adapter.isAdmin());
        }

        @Test
        void shouldReturnFalseWhenContextIsEmpty() {
            SecurityContextHolder.clearContext();

            assertFalse(adapter.isAdmin());
        }
    }

    @Nested
    class RefreshAuthenticationTests {

        @Test
        void shouldUpdateSecurityContextWithCorrectAuthorities() {
            var user = createUser(UserRole.MEMBER, true);

            adapter.refreshAuthentication(user);

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertEquals("testuser", auth.getName());
            assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MEMBER")));
            assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("EMAIL_VERIFIED")));
        }

        @Test
        void shouldNotIncludeEmailVerifiedWhenFalse() {
            var user = createUser(UserRole.ADMIN, false);

            adapter.refreshAuthentication(user);

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
            assertFalse(auth.getAuthorities().contains(new SimpleGrantedAuthority("EMAIL_VERIFIED")));
        }
    }
}
