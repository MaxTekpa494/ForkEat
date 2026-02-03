package fr.uge.forkeat.infrastructure.security;

import fr.uge.forkeat.infrastructure.security.strategy.PrincipalExtractor;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adaptateur pour l'authentification. Délègue l'extraction à des stratégies
 * spécialisées.
 */
@Component
public class AuthenticationAdapter implements AuthenticationPort {

    private final List<PrincipalExtractor> extractors;

    public AuthenticationAdapter(List<PrincipalExtractor> extractors) {
        this.extractors = extractors;
    }

    @Override
    public String extractUsername(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication cannot be null");
        }

        Object principal = authentication.getPrincipal();

        return extractors.stream().filter(extractor -> extractor.supports(principal)).findFirst()
                .map(extractor -> extractor.extractUsername(principal)).orElse(authentication.getName());
    }

    @Override
    public User extractUser(Authentication authentication) {
        if (authentication == null)
            return null;

        Object principal = authentication.getPrincipal();

        return extractors.stream().filter(extractor -> extractor.supports(principal)).findFirst()
                .map(extractor -> extractor.extractUser(principal)).orElse(null);
    }

    @Override
    public boolean isOAuth2Authentication(Authentication authentication) {
        if (authentication == null)
            return false;

        Object principal = authentication.getPrincipal();

        return extractors.stream().filter(extractor -> extractor.supports(principal)).findFirst()
                .map(extractor -> extractor.isOAuth2(principal)).orElse(false);
    }

    @Override
    public void refreshAuthentication(User user) {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(user.role().name()));

        var newAuth = new UsernamePasswordAuthenticationToken(user.username(), authorities);

        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }
}