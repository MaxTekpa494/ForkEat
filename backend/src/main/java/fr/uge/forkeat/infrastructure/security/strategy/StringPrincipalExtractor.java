package fr.uge.forkeat.infrastructure.security.strategy;

import fr.uge.forkeat.service.model.User;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class StringPrincipalExtractor implements PrincipalExtractor {
    
    @Override
    public boolean supports(Object principal) {
        return principal instanceof String;
    }
    
    @Override
    public String extractUsername(Object principal) {
        if (principal instanceof String username) {
            return username;
        }
        throw new UnsupportedOperationException(
            "StringPrincipalExtractor ne supporte que String"
        );
    }
    
    @Override
    public User extractUser(Object principal) {
        return null;
    }
    
    @Override
    public boolean isOAuth2(Object principal) {
        return false;
    }
}