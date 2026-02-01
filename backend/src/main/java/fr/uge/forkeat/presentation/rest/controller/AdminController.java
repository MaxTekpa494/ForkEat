package fr.uge.forkeat.presentation.rest.controller;


import fr.uge.forkeat.presentation.rest.dto.UserRegister;
import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import fr.uge.forkeat.service.model.UserRole;
import fr.uge.forkeat.service.user.UserRegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRegistrationService userService;

    public  AdminController(UserRegistrationService userService) {
        this.userService = userService;
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
    public ResponseEntity<?> registerUser(@RequestBody UserRegister moderatorRegister) throws ResourceNotFoundException {
        this.userService.registerUser(moderatorRegister.firstName(), moderatorRegister.lastName(), moderatorRegister.username(), moderatorRegister.email(), moderatorRegister.password(), UserRole.MODERATOR);
        return ResponseEntity.ok().build();
    }
}
