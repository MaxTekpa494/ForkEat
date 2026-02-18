package fr.uge.forkeat.infrastructure.config;

import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtUtils jwtUtils;

    public JwtFilter(CustomUserDetailsService customUserDetailsService, JwtUtils jwtUtils) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = request.getHeader("Authorization");
        String username = null;
        String tokenBody = null;

        // Extract the username of the token
        if (token != null && token.startsWith("Bearer ")) {
            tokenBody =  token.substring(7);
            try {
                username = jwtUtils.extractUsername(tokenBody);
            } catch (ExpiredJwtException e) {
                // Token expired: don't authenticate, but let the filter chain continue.
                // Spring Security will decide based on permitAll() vs authenticated().
                response.setHeader("X-Token-Expired", "true");
                filterChain.doFilter(request, response);
                return;
            }
        }

        // We recup the userDetails
        if(username != null && SecurityContextHolder.getContext().getAuthentication() == null){
            var userDetails = customUserDetailsService.loadUserByUsername(username);

            //We authenticate the user
            if(jwtUtils.validateToken(tokenBody, userDetails)) {
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        filterChain.doFilter(request, response);


    }
}
