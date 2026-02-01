package fr.uge.forkeat.presentation.web.controller;

import fr.uge.forkeat.service.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        ex.printStackTrace();
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("pageTitle", "Page non trouvée");
        return "error/404";
    }

}