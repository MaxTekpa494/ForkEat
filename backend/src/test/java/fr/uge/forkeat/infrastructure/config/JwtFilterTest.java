package fr.uge.forkeat.infrastructure.config;

import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtFilter(userDetailsService, jwtUtils);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class ValidTokenTests {

        @Test
        void shouldSetAuthenticationForValidBearerToken() throws Exception {
            // Given
            var tokenBody = "valid.jwt.token";
            var userDetails = User.withUsername("testuser")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_MEMBER")))
                    .build();

            when(request.getHeader("Authorization")).thenReturn("Bearer " + tokenBody);
            when(jwtUtils.extractUsername(tokenBody)).thenReturn("testuser");
            when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
            when(jwtUtils.validateToken(tokenBody, userDetails)).thenReturn(true);

            // When
            jwtFilter.doFilterInternal(request, response, filterChain);

            // Then
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertEquals("testuser", auth.getName());
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    class NoTokenTests {

        @Test
        void shouldPassThroughWhenNoAuthorizationHeader() throws Exception {
            // Given
            when(request.getHeader("Authorization")).thenReturn(null);

            // When
            jwtFilter.doFilterInternal(request, response, filterChain);

            // Then
            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtUtils, userDetailsService);
        }

        @Test
        void shouldPassThroughWhenHeaderNotBearer() throws Exception {
            // Given
            when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

            // When
            jwtFilter.doFilterInternal(request, response, filterChain);

            // Then
            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    class InvalidTokenTests {

        @Test
        void shouldPassThroughWhenTokenValidationFails() throws Exception {
            // Given
            var tokenBody = "invalid.jwt.token";
            var userDetails = User.withUsername("testuser")
                    .password("password")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_MEMBER")))
                    .build();

            when(request.getHeader("Authorization")).thenReturn("Bearer " + tokenBody);
            when(jwtUtils.extractUsername(tokenBody)).thenReturn("testuser");
            when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);
            when(jwtUtils.validateToken(tokenBody, userDetails)).thenReturn(false);

            // When
            jwtFilter.doFilterInternal(request, response, filterChain);

            // Then
            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verify(filterChain).doFilter(request, response);
        }
    }
}
