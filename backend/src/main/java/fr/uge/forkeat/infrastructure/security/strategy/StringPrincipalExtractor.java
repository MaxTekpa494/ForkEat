package fr.uge.forkeat.infrastructure.security.strategy;

import fr.uge.forkeat.service.model.User;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Order(1)
public class StringPrincipalExtractor implements PrincipalExtractor {
    
    @Override
    public boolean supports(Object principal) {
        Objects.requireNonNull(principal);
        return principal instanceof String;
    }
    
    @Override
    public String extractUsername(Object principal) {
        Objects.requireNonNull(principal);
        return switch (principal){
            case String username -> username;
            default -> throw new UnsupportedOperationException(
                    "StringPrincipalExtractor ne supporte que String"
            );
        };
    }
    
    @Override
    public User extractUser(Object principal) {
        return null;
    } // normal ici on cherche pas à extraire user
    
    @Override
    public boolean isOAuth2(Object principal) {
        return false;
    }
}