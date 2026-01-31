package fr.uge.forkeat.infrastructure.security.strategy;

import fr.uge.forkeat.service.model.User;

public interface PrincipalExtractor {

    boolean supports(Object principal);
    String extractUsername(Object principal);
    User extractUser(Object principal);
    boolean isOAuth2(Object principal);
}