package fr.uge.forkeat.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()) {
            boolean hasEmailVerified = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("EMAIL_VERIFIED"));

            if (!hasEmailVerified) {
                response.sendRedirect(request.getContextPath() + "/account/email-verification-required");
                return;
            }
        }

        response.sendRedirect(request.getContextPath() + "/error/403");
    }
}
