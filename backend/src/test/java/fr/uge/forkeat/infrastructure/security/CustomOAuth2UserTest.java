package fr.uge.forkeat.infrastructure.security;

import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomOAuth2UserTest {

    private OAuth2User wrappedOAuth2User;
    private User domainUser;
    private CustomOAuth2User customOAuth2User;

    @BeforeEach
    void setUp() {
        wrappedOAuth2User = mock(OAuth2User.class);
        domainUser = new User(UUID.randomUUID(), "chef_arnaud", "Arnaud", "Carayol",
                "arnaud@test.com", UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.GOOGLE,
                Instant.now(), Instant.now(), true);
        customOAuth2User = new CustomOAuth2User(wrappedOAuth2User, domainUser);
    }

    @Test
    void shouldReturnCorrectAuthorities() {
        var authorities = customOAuth2User.getAuthorities();
        assertEquals(2, authorities.size());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_MEMBER")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("EMAIL_VERIFIED")));
    }

    @Test
    void shouldReturnAdminRoleForAdminUser() {
        var adminUser = new User(UUID.randomUUID(), "admin", "Admin", "User",
                "admin@test.com", UserRole.ADMIN, UserStatus.ACTIVE, AuthMode.GOOGLE,
                Instant.now(), Instant.now(), true);
        var adminOAuth2 = new CustomOAuth2User(wrappedOAuth2User, adminUser);

        assertTrue(adminOAuth2.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void shouldReturnUsernameAsName() {
        assertEquals("chef_arnaud", customOAuth2User.getName());
    }

    @Test
    void shouldDelegateGetAttributes() {
        var attrs = Map.<String, Object>of("email", "arnaud@test.com");
        when(wrappedOAuth2User.getAttributes()).thenReturn(attrs);

        assertEquals(attrs, customOAuth2User.getAttributes());
        verify(wrappedOAuth2User).getAttributes();
    }

    @Test
    void shouldReturnDomainUser() {
        assertEquals(domainUser, customOAuth2User.getUser());
    }
}
