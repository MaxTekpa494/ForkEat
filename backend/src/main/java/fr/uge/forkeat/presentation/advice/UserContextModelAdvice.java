package fr.uge.forkeat.presentation.advice;

import fr.uge.forkeat.service.port.AuthenticationPort;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class UserContextModelAdvice {

    private final AuthenticationPort authPort;

    public UserContextModelAdvice(AuthenticationPort authPort) {
        this.authPort = authPort;
    }

    @ModelAttribute("isModerator")
    public boolean isModerator() {
        try {
            return authPort.isModerator();
        } catch (Exception e) {
            return false;
        }
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin() {
        try {
            return authPort.isAdmin();
        } catch (Exception e) {
            return false;
        }
    }

    @ModelAttribute("isAuthenticated")
    public boolean isAuthenticated() {
        try {
            return authPort.isAuthenticated();
        } catch (Exception e) {
            return false;
        }
    }

    @ModelAttribute("currentUsername")
    public String currentUsername() {
        try {
            return authPort.isAuthenticated() ? authPort.extractUsername() : "";
        } catch (Exception e) {
            return "";
        }
    }
}
