package fr.uge.forkeat.infrastructure.config;

import fr.uge.forkeat.infrastructure.security.CustomOAuth2UserService;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
@PropertySource("classpath:bucket4j.properties")
@EnableConfigurationProperties(RateLimitProperties.class)
public class SecurityConfig {

	private final CustomUserDetailsService customUserDetailsService;
	private final JwtUtils jwtUtils;
	private final CustomOAuth2UserService customOAuth2UserService;
	private final PasswordEncoder passwordEncoder;
	private final RateLimitProperties rateLimitProperties;
	private final fr.uge.forkeat.infrastructure.security.CustomAccessDeniedHandler customAccessDeniedHandler;

	public SecurityConfig(CustomUserDetailsService customUserDetailsService, JwtUtils jwtUtils,
			CustomOAuth2UserService customOAuth2UserService, PasswordEncoder passwordEncoder,
			RateLimitProperties rateLimitProperties,
			fr.uge.forkeat.infrastructure.security.CustomAccessDeniedHandler customAccessDeniedHandler) {
		this.customUserDetailsService = customUserDetailsService;
		this.jwtUtils = jwtUtils;
		this.customOAuth2UserService = customOAuth2UserService;
		this.passwordEncoder = passwordEncoder;
		this.rateLimitProperties = rateLimitProperties;
		this.customAccessDeniedHandler = customAccessDeniedHandler;
	}

	@Bean
	public AuthenticationManager authenticationManager(HttpSecurity http) {
		AuthenticationManagerBuilder authenticationManagerBuilder = http
				.getSharedObject(AuthenticationManagerBuilder.class);
		authenticationManagerBuilder.userDetailsService(customUserDetailsService).passwordEncoder(passwordEncoder);
		return authenticationManagerBuilder.build();
	}

	@Bean
	@Order(1)
	public SecurityFilterChain apiFilterChain(HttpSecurity http) {
		return http.securityMatcher("/api/**").csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, authException) -> {
							response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
							response.setContentType("application/json");
							response.getWriter().write("{\"error\": \"Unauthorized\"}");
						}))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers("/api/recipes/*/like").hasAuthority("EMAIL_VERIFIED")
						.requestMatchers("/api/auth/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/promotions/active", "/api/promotions/upcoming").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/super-likes/price").permitAll()
						.requestMatchers("/api/promotions/stream").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/recipes/**").permitAll()
						.requestMatchers("/api/recipes/**").hasAuthority("EMAIL_VERIFIED")
						.requestMatchers("/api/wallet/**").hasAuthority("EMAIL_VERIFIED")
						.requestMatchers("/api/redistribution/**").hasAuthority("EMAIL_VERIFIED")
						.requestMatchers("/api/account/**").authenticated()
						.requestMatchers("/api/profile/**").authenticated()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.requestMatchers("/api/moderator/**").hasRole("MODERATOR")
						.requestMatchers("/api/user/**").authenticated()
						.anyRequest().authenticated())
				.addFilterBefore(new RateLimitFilter(rateLimitProperties), UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(new JwtFilter(customUserDetailsService, jwtUtils),
								 UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain webFilterChain(HttpSecurity http) {
		return http
				// On garde CSRF désactivé pour le développement il faut pense a le réactiver
				.csrf(AbstractHttpConfigurer::disable)

				.exceptionHandling(ex -> ex
						.accessDeniedHandler(customAccessDeniedHandler))

				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/", "/auth/**", "/login", "/error/**", "/css/**", "/js/**", "/images/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/recipes/create", "/recipes/create-variant").hasAuthority("EMAIL_VERIFIED")
						.requestMatchers(HttpMethod.GET, "/recipes/**").permitAll()
						.requestMatchers("/recipes/my").authenticated()
						.requestMatchers("/recipes/create", "/recipes/*/edit").hasAuthority("EMAIL_VERIFIED")
						.requestMatchers("/recipes/*/delete").authenticated() // edit et delete c pas EMAIL verified
																				// à corriger quand on les fait

						.requestMatchers("/wallet/webhooks/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/wallet").authenticated()
						.requestMatchers("/wallet/**").hasAuthority("EMAIL_VERIFIED")
						.requestMatchers("/admin/**").hasRole("ADMIN")
						.requestMatchers("/moderator/**").hasRole("MODERATOR")
						.requestMatchers("/profile/**").authenticated()
						.anyRequest().authenticated())

				.addFilterBefore(new RateLimitFilter(rateLimitProperties), UsernamePasswordAuthenticationFilter.class)

				.formLogin(form -> form
						.loginPage("/auth/login")
						.loginProcessingUrl("/auth/login")
						.successHandler((request, response, authentication) -> {
							var isAdmin = authentication.getAuthorities().stream()
									.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
							response.sendRedirect(isAdmin ? "/admin" : "/recipes");
						})
						.failureUrl("/auth/login?error=true")
						.permitAll())

				.oauth2Login(oauth2 -> oauth2
						.loginPage("/auth/login")
						.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
						.successHandler((request, response, authentication) -> {
							var isAdmin = authentication.getAuthorities().stream()
									.anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
							response.sendRedirect(isAdmin ? "/admin" : "/recipes");
						})
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
