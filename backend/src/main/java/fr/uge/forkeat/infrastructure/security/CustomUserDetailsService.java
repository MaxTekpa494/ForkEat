package fr.uge.forkeat.infrastructure.security;

import fr.uge.forkeat.service.persistence.UserPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Objects;

@Component
public class CustomUserDetailsService implements UserDetailsService {

    private final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserPersistence userPersistence;

    public CustomUserDetailsService(UserPersistence userPersistence) {
        this.userPersistence = userPersistence;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Objects.requireNonNull(username);
        logger.debug("Loading user by username or email: {}", username);

        var user = userPersistence.findByUsername(username)
                .or(() -> userPersistence.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        logger.debug("Loading user by username: {}", username);
        // Un user Google sans password local → on met un placeholder vide
        // qui ne matchera jamais avec BCrypt, donc le form login échouera proprement.
        var password = userPersistence.findPasswordHashByUsername(user.username());
        var resolvedPassword = password != null ? password : "";

        var authorities = new ArrayList<SimpleGrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.role().toString()));
        if (user.emailVerified()) {
            authorities.add(new SimpleGrantedAuthority("EMAIL_VERIFIED"));
        }

        return User.withUsername(user.username())
                .password(resolvedPassword)
                .authorities(authorities)
                .build();
    }
}