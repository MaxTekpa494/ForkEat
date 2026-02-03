package fr.uge.forkeat.infrastructure.security.strategy;

import fr.uge.forkeat.service.model.user.User;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class StringPrincipalExtractor implements PrincipalExtractor {

	@Override
    public boolean supports(Object principal) {
        return switch (principal) {
            case String _ -> true;
            default -> false;
        };
    }

	@Override
    public String extractUsername(Object principal) {
        return switch (principal) {
            case String username -> username;
            default -> throw new UnsupportedOperationException("StringPrincipalExtractor ne supporte que String");
        };
    }

	@Override
	public User extractUser(Object principal) {
		return null; // not supposed to happen
	}

	@Override
	public boolean isOAuth2(Object principal) {
		return false;
	}
}