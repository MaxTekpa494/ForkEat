package fr.uge.forkeat.presentation.rest.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/moderator")
public class ModeratorController {
    @GetMapping("greeting")
    public ResponseEntity<?> greeting(){
        return ResponseEntity.ok().body("Hello Moderator!");
    }
}
