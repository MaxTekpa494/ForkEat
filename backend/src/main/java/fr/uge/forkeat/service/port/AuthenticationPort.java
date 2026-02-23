package fr.uge.forkeat.service.port;

import fr.uge.forkeat.service.model.user.User;
import org.springframework.security.core.Authentication;

public interface AuthenticationPort {
    String extractUsername(); // Pour les autres, il faut faire comme lui et pas dépendre de Authentication
    User extractUser(Authentication authentication);
    boolean isOAuth2Authentication(Authentication authentication);
    void refreshAuthentication(User user);
    String generateToken(String username);
}