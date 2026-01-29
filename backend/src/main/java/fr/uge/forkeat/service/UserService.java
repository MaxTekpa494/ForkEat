package fr.uge.forkeat.service;

import fr.uge.forkeat.infrastructure.persistence.postgres.entity.UserEntity;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserJpaRepository;
import fr.uge.forkeat.infrastructure.security.JwtUtils;
import fr.uge.forkeat.presentation.rest.dto.UserLogin;
import fr.uge.forkeat.presentation.rest.dto.UserRegister;
import fr.uge.forkeat.service.model.AuthMode;
import fr.uge.forkeat.service.model.UserRole;
import fr.uge.forkeat.service.model.UserStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    private final UserJpaRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;


    public UserService(UserJpaRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    public boolean registerUser(UserRegister userRegister, UserRole role){
        if(userRepository.findByUsername(userRegister.username()).isPresent()){
            return false;
        }

        var user = new UserEntity(userRegister.username(), userRegister.firstName(), userRegister.lastName(), passwordEncoder.encode(userRegister.password()), userRegister.email(), role, UserStatus.ACTIVE, AuthMode.LOCAL);
        userRepository.save(user);
        return true;
    }

    public Optional<Map<String, Object>> loginUser(UserLogin userLogin){
        try{
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userLogin.username(), userLogin.password()));
            if(authentication.isAuthenticated()){
                HashMap<String, Object> authData = new HashMap<>();
                authData.put("token", jwtUtils.generateToken(userLogin.username()));
                authData.put("type", "Bearer");
                return Optional.of(authData);
            }
            return Optional.empty();

        }catch(AuthenticationException e){
            return Optional.empty();
        }
    }
}
