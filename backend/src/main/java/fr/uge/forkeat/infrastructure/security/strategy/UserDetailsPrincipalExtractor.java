package fr.uge.forkeat.infrastructure.security.strategy;

import fr.uge.forkeat.service.model.User;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Objects;


@Component
@Order(1)
public class UserDetailsPrincipalExtractor implements PrincipalExtractor {
    
    @Override
    public boolean supports(Object principal) {
        Objects.requireNonNull(principal);
        return principal instanceof UserDetails;
    }
    
    @Override
    public String extractUsername(Object principal) {
        Objects.requireNonNull(principal);
        return switch (principal){
            case UserDetails userDetails ->  userDetails.getUsername();
            default -> throw new UnsupportedOperationException(
                    "UserDetailsPrincipalExtractor ne supporte que UserDetails"
            );
        };
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