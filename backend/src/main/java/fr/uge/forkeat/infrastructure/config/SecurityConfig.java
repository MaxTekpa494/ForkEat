package fr.uge.forkeat.infrastructure.config;

import fr.uge.forkeat.infrastructure.security.CustomOAuth2UserService;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final CustomUserDetailsService customUserDetailsService;
	private final JwtUtils jwtUtils;
	private final CustomOAuth2UserService customOAuth2UserService;
	private final PasswordEncoder passwordEncoder;

	public SecurityConfig(CustomUserDetailsService customUserDetailsService, JwtUtils jwtUtils,
			CustomOAuth2UserService customOAuth2UserService, PasswordEncoder passwordEncoder) {
		this.customUserDetailsService = customUserDetailsService;
		this.jwtUtils = jwtUtils;
		this.customOAuth2UserService = customOAuth2UserService;
		this.passwordEncoder = passwordEncoder;
	}

	@Bean
	public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
		AuthenticationManagerBuilder authenticationManagerBuilder = http
				.getSharedObject(AuthenticationManagerBuilder.class);
		authenticationManagerBuilder.userDetailsService(customUserDetailsService).passwordEncoder(passwordEncoder);
		return authenticationManagerBuilder.build();
	}

	@Bean
	@Order(1)
	public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
		return http.securityMatcher("/api/**").csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, authException) -> {
							response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
							response.setContentType("application/json");
							response.getWriter().write("{\"error\": \"Unauthorized\"}");
						}))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/auth/**").permitAll().requestMatchers("/*/open/*").permitAll()
						.requestMatchers("/*/user/*").authenticated().requestMatchers("/*/moderator/*")
						.hasRole("MODERATOR").requestMatchers("/*/admin/*").hasRole("ADMIN").anyRequest()
						.hasRole("ADMIN"))
				.addFilterBefore(new JwtFilter(customUserDetailsService, jwtUtils),
								 UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
		return http
				// On garde CSRF désactivé pour le développement il faut pense a le réactiver
				.csrf(AbstractHttpConfigurer::disable)

				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/", "/auth/**", "/login", "/register", "/css/**", "/js/**", "/images/**").permitAll()
						.requestMatchers("/admin/**").hasRole("ADMIN")
						.requestMatchers("/moderator/**").hasRole("MODERATOR")
						.requestMatchers("/profile/**").authenticated()
						.anyRequest().authenticated())

				.formLogin(form -> form
						.loginPage("/auth/login")
						.loginProcessingUrl("/auth/login")
						.defaultSuccessUrl("/dashboard", true) // il faut mettre /recipes ici
						.failureUrl("/auth/login?error=true")
						.permitAll())

				.oauth2Login(oauth2 -> oauth2
						.loginPage("/auth/login")
						.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
						.defaultSuccessUrl("/dashboard", true)
						.failureUrl("/auth/login?error=true"))

				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/auth/login?logout=true")
						.invalidateHttpSession(true)
						.deleteCookies("JSESSIONID")
						.permitAll())
				.build();
	}

	@Bean
	public RoleHierarchy roleHierarchy() {
		return RoleHierarchyImpl.fromHierarchy("ROLE_ADMIN > ROLE_MODERATOR \n ROLE_MODERATOR > ROLE_MEMBER");
	}
}