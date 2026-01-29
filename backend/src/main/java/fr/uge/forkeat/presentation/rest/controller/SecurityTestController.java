package fr.uge.forkeat.presentation.rest.controller;


import fr.uge.forkeat.presentation.rest.dto.UserLogin;
import fr.uge.forkeat.presentation.rest.dto.UserRegister;
import fr.uge.forkeat.service.UserService;
import fr.uge.forkeat.service.model.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
public class SecurityTestController {

    private final UserService userService;

    public SecurityTestController( UserService userService) {
        this.userService = userService;
    }

    /**
     *  Function corresponding to the endpoint used to register a new user
     * @param userRegister A DTO which represent the data for the registering of a user
     * @return the user newly created Must return a DTO instead
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegister userRegister) {
        if(this.userService.registerUser(userRegister, UserRole.MEMBER)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().body("Username is already used");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLogin userLogin) {
        var authData = this.userService.loginUser(userLogin);
        if(authData.isPresent()) {
            return ResponseEntity.ok(authData.get());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
    }

}
