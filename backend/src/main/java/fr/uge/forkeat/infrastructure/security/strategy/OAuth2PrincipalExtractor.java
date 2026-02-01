package fr.uge.forkeat.infrastructure.security.strategy;

import fr.uge.forkeat.infrastructure.security.CustomOAuth2User;
import fr.uge.forkeat.infrastructure.security.CustomOAuth2UserService;
import fr.uge.forkeat.service.model.User;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Order(1)
public class OAuth2PrincipalExtractor implements PrincipalExtractor {
    
    @Override
    public boolean supports(Object principal) {
        Objects.requireNonNull(principal);
        return principal instanceof CustomOAuth2User;
    }
    
    @Override
    public String extractUsername(Object principal) {
        Objects.requireNonNull(principal);
        return switch (principal){
            case CustomOAuth2User oauth2User -> oauth2User.getUser().username();
            default ->  throw new UnsupportedOperationException(
                    "OAuth2PrincipalExtractor ne supporte que CustomOAuth2User"
            );
        };
    }
    
    @Override
    public User extractUser(Object principal) {
        Objects.requireNonNull(principal);
        return switch (principal){
            case CustomOAuth2User oauth2User -> oauth2User.getUser();
            default ->  throw new UnsupportedOperationException(
                    "Something went wrong when extracting user from OAuth2"
            );
        };
    }
    
    @Override
    public boolean isOAuth2(Object principal) {
        return true;
    }
}