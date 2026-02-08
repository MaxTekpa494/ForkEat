package fr.uge.forkeat.infrastructure.security;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.user.UserQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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

        // Un user Google sans password local → on met un placeholder vide
        // qui ne matchera jamais avec BCrypt, donc le form login échouera proprement
        var password = user.getPassword() != null ? user.getPassword() : "";

        return  User.withUsername(user.getUsername())
                .password(password)
                .roles(user.getRole().toString())
                .build();
    }
}
