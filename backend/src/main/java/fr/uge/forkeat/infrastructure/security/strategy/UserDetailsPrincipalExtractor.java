package fr.uge.forkeat.infrastructure.security.strategy;

import fr.uge.forkeat.service.model.user.User;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class UserDetailsPrincipalExtractor implements PrincipalExtractor {

	@Override
    public boolean supports(Object principal) {
        return switch (principal) {
            case UserDetails _ -> true;
            default -> false;
        };
    }

	@Override
    public String extractUsername(Object principal) {
        return switch (principal) {
            case UserDetails userDetails -> userDetails.getUsername();
            default -> throw new UnsupportedOperationException("UserDetailsPrincipalExtractor ne supporte que UserDetails");
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