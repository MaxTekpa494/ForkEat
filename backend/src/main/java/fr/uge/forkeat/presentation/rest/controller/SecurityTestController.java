package fr.uge.forkeat.presentation.rest.controller;


import fr.uge.forkeat.presentation.rest.dto.UserLogin;
import fr.uge.forkeat.presentation.rest.dto.UserRegister;
import fr.uge.forkeat.infrastructure.security.JwtUtils;
import fr.uge.forkeat.infrastructure.persistence.postgres.entity.User;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
public class SecurityTestController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public SecurityTestController(AuthenticationManager authenticationManager,  JwtUtils jwtUtils, PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    /**
     *  Function corresponding to the endpoint used to register a new user
     * @param userRegister A DTO which represent the data for the registering of a user
     * @return the user newly created Must return a DTO instead
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegister userRegister) {
        if(userRepository.findByUsername(userRegister.username()).isPresent()){
            return ResponseEntity.badRequest().body("Username is already used");
        }

        var user = new User(userRegister.username(), passwordEncoder.encode(userRegister.password()), userRegister.email(), "USER");
        return ResponseEntity.ok(userRepository.save(user));
    }

    @PostMapping("login")
    public ResponseEntity<?> login(@RequestBody UserLogin userLogin) {
        try{
            System.out.println(userLogin);
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userLogin.username(), userLogin.password()));
            if(authentication.isAuthenticated()){
                HashMap<String, Object> authData = new HashMap<>();
                authData.put("token", jwtUtils.generateToken(userLogin.username()));
                authData.put("type", "Bearer");
                return ResponseEntity.ok(authData);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");

        }catch(AuthenticationException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }
    }

}
