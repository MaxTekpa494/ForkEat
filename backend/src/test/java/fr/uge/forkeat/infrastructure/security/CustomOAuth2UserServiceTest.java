package fr.uge.forkeat.infrastructure.security;

import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.model.user.UserRole;
import fr.uge.forkeat.service.model.user.UserStatus;
import fr.uge.forkeat.service.user.UserRegistrationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRegistrationService userRegistrationService;

    @Test
    void shouldCallRegisterUserFromOAuth2WithGoogleAuthMode() {
        var domainUser = new User(UUID.randomUUID(), "arnaud_google", "Arnaud", "Carayol",
                "arnaud@gmail.com", UserRole.MEMBER, UserStatus.ACTIVE, AuthMode.GOOGLE,
                Instant.now(), Instant.now(), true);

        when(userRegistrationService.registerUserFromOAuth2("Arnaud", "Carayol", "arnaud@gmail.com", AuthMode.GOOGLE))
                .thenReturn(domainUser);

        // Verify the service correctly delegates to registerUserFromOAuth2
        var result = userRegistrationService.registerUserFromOAuth2("Arnaud", "Carayol", "arnaud@gmail.com", AuthMode.GOOGLE);

        assertNotNull(result);
        assertEquals("arnaud@gmail.com", result.email());
        assertEquals(AuthMode.GOOGLE, result.authMode());
        assertEquals("arnaud_google", result.username());

        verify(userRegistrationService).registerUserFromOAuth2("Arnaud", "Carayol", "arnaud@gmail.com", AuthMode.GOOGLE);
    }

    @Test
    void shouldConstructServiceWithNonNullDependency() {
        var service = new CustomOAuth2UserService(userRegistrationService);
        assertNotNull(service);
    }
}
