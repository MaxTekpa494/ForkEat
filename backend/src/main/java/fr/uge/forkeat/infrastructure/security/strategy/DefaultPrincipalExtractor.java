package fr.uge.forkeat.infrastructure.security.strategy;

import fr.uge.forkeat.service.model.user.User;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DefaultPrincipalExtractor implements PrincipalExtractor {
    
    @Override
    public boolean supports(Object principal) {
        return true;
    }
    
    @Override
    public String extractUsername(Object principal) {
        return principal != null ? principal.toString() : "unknown";
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