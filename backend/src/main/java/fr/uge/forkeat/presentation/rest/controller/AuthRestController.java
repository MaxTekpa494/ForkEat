package fr.uge.forkeat.presentation.rest.controller;


import fr.uge.forkeat.infrastructure.config.JwtUtils;
import fr.uge.forkeat.presentation.rest.dto.UserLogin;
import fr.uge.forkeat.presentation.rest.dto.UserRegister;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.UserRole;
import fr.uge.forkeat.service.user.UserRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    private final UserRegistrationService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public AuthRestController(UserRegistrationService userService, AuthenticationManager authenticationManager, JwtUtils jwtUtils) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    /**
     *  Function corresponding to the endpoint used to register a new user
     * @param userRegister A DTO which represent the data for the registering of a user
     * @return the user newly created Must return a DTO instead
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegister userRegister) throws ResourceNotFoundException {
        this.userService.registerUser(userRegister.firstName(), userRegister.lastName(), userRegister.username(), userRegister.email(), userRegister.password(), UserRole.MEMBER);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLogin userLogin) {
        try{
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
