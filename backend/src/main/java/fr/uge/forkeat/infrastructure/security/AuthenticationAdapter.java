package fr.uge.forkeat.infrastructure.security;

import fr.uge.forkeat.infrastructure.config.JwtUtils;
import fr.uge.forkeat.infrastructure.security.strategy.PrincipalExtractor;
import fr.uge.forkeat.service.model.user.User;
import fr.uge.forkeat.service.port.AuthenticationPort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptateur pour l'authentification. Délègue l'extraction à des stratégies
 * spécialisées.
 */
@Component
public class AuthenticationAdapter implements AuthenticationPort {

    private final List<PrincipalExtractor> extractors;
    private final JwtUtils jwtUtils;

    public AuthenticationAdapter(List<PrincipalExtractor> extractors, JwtUtils jwtUtils) {
        this.extractors = extractors;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public String extractUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication context cannot be null");
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
    public String generateToken(String username) {
        return jwtUtils.generateToken(username);
    }

    @Override
    public boolean isAuthenticated() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !(auth.getPrincipal() instanceof String s && s.equals("anonymousUser"));
    }

    @Override
    public boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Override
    public void refreshAuthentication(User user) {
        String roleName = user.role().name().startsWith("ROLE_")
                ? user.role().name()
                : "ROLE_" + user.role().name();

        var authorities = new ArrayList<SimpleGrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority(roleName));
        if (user.emailVerified()) {
            authorities.add(new SimpleGrantedAuthority("EMAIL_VERIFIED"));
        }

        var newAuth = new UsernamePasswordAuthenticationToken(
                user.username(),
                null,
                authorities
        );
        SecurityContextHolder.getContext().setAuthentication(newAuth);
    }
}