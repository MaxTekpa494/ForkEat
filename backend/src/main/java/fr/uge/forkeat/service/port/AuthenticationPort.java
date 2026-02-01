package fr.uge.forkeat.service.port;

import fr.uge.forkeat.service.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface AuthenticationPort {
    String extractUsername(Authentication authentication);
    User extractUser(Authentication authentication);
    boolean isOAuth2Authentication(Authentication authentication);
    void refreshAuthentication(User user);
}