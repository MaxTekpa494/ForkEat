package fr.uge.forkeat.presentation.rest.controller;


import fr.uge.forkeat.infrastructure.persistence.postgres.entity.User;
import fr.uge.forkeat.infrastructure.persistence.postgres.repository.UserRepository;
import fr.uge.forkeat.presentation.rest.dto.UserRegister;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;


    public  AdminController(UserRepository userRepository,  PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/greeting")
    public ResponseEntity<?> greeting(){
        return ResponseEntity.ok().body("Hello Admin!");
    }

    /**
     *  Function corresponding to the endpoint used to register a new moderator
     * @param moderatorRegister A DTO which represent the data for the registering of a moderator
     * @return the user newly created Must return a DTO instead
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegister moderatorRegister) {
        if(userRepository.findByUsername(moderatorRegister.username()).isPresent()){
            return ResponseEntity.badRequest().body("Username is already used");
        }

        var user = new User(moderatorRegister.username(), passwordEncoder.encode(moderatorRegister.password()), moderatorRegister.email(), "MODERATOR");
        return ResponseEntity.ok(userRepository.save(user));
    }
}
