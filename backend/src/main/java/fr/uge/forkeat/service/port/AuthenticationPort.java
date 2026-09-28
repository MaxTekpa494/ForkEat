package fr.uge.forkeat.service.port;

import fr.uge.forkeat.service.model.user.User;
import org.springframework.security.core.Authentication;

public interface AuthenticationPort {
    String extractUsername();
    User extractUser(Authentication authentication);
    boolean isOAuth2Authentication(Authentication authentication);
    void refreshAuthentication(User user);
    String generateToken(String username);
    boolean isAdmin();
    boolean isModerator();
    boolean isAuthenticated();
}