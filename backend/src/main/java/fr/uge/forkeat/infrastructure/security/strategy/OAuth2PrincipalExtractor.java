package fr.uge.forkeat.infrastructure.security.strategy;

import fr.uge.forkeat.infrastructure.security.CustomOAuth2User;
import fr.uge.forkeat.service.model.User;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class OAuth2PrincipalExtractor implements PrincipalExtractor {
    
    @Override
    public boolean supports(Object principal) {
        return principal instanceof CustomOAuth2User;
    }
    
    @Override
    public String extractUsername(Object principal) {
        if (principal instanceof CustomOAuth2User oauth2User) {
            return oauth2User.getUser().username();
        }
        throw new UnsupportedOperationException(
            "OAuth2PrincipalExtractor ne supporte que CustomOAuth2User"
        );
    }
    
    @Override
    public User extractUser(Object principal) {
        if (principal instanceof CustomOAuth2User oauth2User) {
            return oauth2User.getUser();
        }
        return null;
    }
    
    @Override
    public boolean isOAuth2(Object principal) {
        return true;
    }
}