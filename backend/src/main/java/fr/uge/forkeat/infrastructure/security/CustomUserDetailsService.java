package fr.uge.forkeat.infrastructure.security;

import fr.uge.forkeat.service.model.UserStatus;
import fr.uge.forkeat.service.persistence.UserPersistence;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserPersistence userPersistence;

    public CustomUserDetailsService(UserPersistence userPersistence) {
        this.userPersistence = userPersistence;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        var userOptional = userPersistence.findByEmail(identifier);

        if (userOptional.isEmpty()) {
            userOptional = userPersistence.findByUsername(identifier);
        }

        var user = userOptional
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec l'identifiant : " + identifier));

        return User.builder()
                .username(user.username())
                .password(user.password())
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + user.role().name())
                ))
                .accountLocked(user.status() == UserStatus.BANNED)
                .disabled(user.status() == UserStatus.SUSPENDED)
                .build();
    }
}