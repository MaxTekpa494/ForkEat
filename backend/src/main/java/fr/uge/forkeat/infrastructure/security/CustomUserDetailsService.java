package fr.uge.forkeat.infrastructure.security;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.user.UserQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    // ICI Il faut bien utiliser le service car il est garant des règles métiers
    private final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    public  CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // TODO : voir si on peut catch notre propre exception

    @Override
    public UserDetails loadUserByUsername(String username){

        logger.debug("Loading user by username or email: " + username);
        var user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Un user Google sans password local -> on met un placeholder vide
        // qui ne matchera jamais avec BCrypt, donc le form login échouera proprement
        var password = user.getPassword() != null ? user.getPassword() : "";

        var authorities = new ArrayList<SimpleGrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().toString()));
        if (user.isEmailVerified()) {
            authorities.add(new SimpleGrantedAuthority("EMAIL_VERIFIED"));
        }

        return User.withUsername(user.getUsername())
                .password(password)
                .authorities(authorities)
                .build();
    }
}
