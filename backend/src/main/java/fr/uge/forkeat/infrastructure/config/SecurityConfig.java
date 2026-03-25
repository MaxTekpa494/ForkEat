package fr.uge.forkeat.infrastructure.config;

import fr.uge.forkeat.infrastructure.security.CustomOAuth2UserService;
import fr.uge.forkeat.infrastructure.security.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;


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

	@Value("${security.csrf.enabled:false}")
	private boolean csrfEnabled;

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
		http.csrf(AbstractHttpConfigurer::disable);

		return http.securityMatcher("/api/**")
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, authException) -> {
							response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
							response.setContentType("application/json");
							response.getWriter().write("{\"error\": \"Unauthorized\"}");
						}))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/wallet/webhooks/**").permitAll()
						.requestMatchers("/api/auth/me").authenticated()
						.requestMatchers("/api/auth/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/promotions/active", "/api/promotions/upcoming").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/super-likes/price").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/super-likes/history").hasAuthority("EMAIL_VERIFIED")
						.requestMatchers(HttpMethod.POST, "/api/super-likes/**").hasAuthority("EMAIL_VERIFIED")
						.requestMatchers(HttpMethod.GET, "/api/promotions/stream").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/recipes/following").hasAuthority("EMAIL_VERIFIED")
						.requestMatchers(HttpMethod.GET, "/api/recipes/my-recipes").hasAuthority("EMAIL_VERIFIED")
						.requestMatchers(HttpMethod.GET, "/api/recipes/**").permitAll()
						.requestMatchers("/api/recipes/**").hasAuthority("EMAIL_VERIFIED")
						.requestMatchers("/api/wallet/**").hasAuthority("EMAIL_VERIFIED")
						.requestMatchers("/api/redistribution/**").hasAuthority("EMAIL_VERIFIED")
						.requestMatchers(HttpMethod.POST, "/api/profile/**").hasAuthority("EMAIL_VERIFIED")
						.requestMatchers(HttpMethod.PUT, "/api/profile/**").hasAuthority("EMAIL_VERIFIED")
						.requestMatchers(HttpMethod.DELETE, "/api/profile/**").hasAuthority("EMAIL_VERIFIED")
						.requestMatchers(HttpMethod.GET, "/api/account/**").authenticated()
						.requestMatchers("/api/account/email-confirmations").authenticated()
						.requestMatchers(HttpMethod.POST, "/api/account/email-change-requests").authenticated()
						.requestMatchers(HttpMethod.PUT, "/api/account/email-change-requests").authenticated()
						.requestMatchers(HttpMethod.POST, "/api/account/delete-requests").authenticated()
						.requestMatchers(HttpMethod.PUT, "/api/account/delete-requests").authenticated()
						.requestMatchers("/api/account/**").hasAuthority("EMAIL_VERIFIED")
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
				if (csrfEnabled) {
					XorCsrfTokenRequestAttributeHandler requestHandler = new XorCsrfTokenRequestAttributeHandler();
					requestHandler.setCsrfRequestAttributeName(null);
					http.csrf(csrf -> csrf
									.csrfTokenRequestHandler(requestHandler)
									.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
									.ignoringRequestMatchers("/wallet/webhooks/**"));
				} else {
					http.csrf(AbstractHttpConfigurer::disable);
				}

				return http.exceptionHandling(ex -> ex
						.accessDeniedHandler(customAccessDeniedHandler))

				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/error/**", "/css/**", "/js/**", "/images/**", "/favicon.svg", "/favicon.ico").permitAll() // Injection js ?
						.requestMatchers("/").permitAll()
						.requestMatchers("/auth/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/recipes/my-recipes").hasAuthority("EMAIL_VERIFIED")
						.requestMatchers(HttpMethod.GET, "/recipes/create", "/recipes/create-variant", "/recipes/following",
														 "/recipes/smart-search", "/recipes/*/edit").hasAuthority("EMAIL_VERIFIED")
						.requestMatchers(HttpMethod.GET, "/recipes/**").permitAll()
						.requestMatchers("/recipes/**").hasAuthority("EMAIL_VERIFIED")

						.requestMatchers("/wallet/webhooks/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/wallet").authenticated()
						.requestMatchers("/wallet/**").hasAuthority("EMAIL_VERIFIED")
						.requestMatchers("/admin/**").hasRole("ADMIN")
						.requestMatchers("/moderator/**").hasRole("MODERATOR")
						.requestMatchers(HttpMethod.POST, "/profile/*/follow", "/profile/*/unfollow", "/profile/*/report").hasAuthority("EMAIL_VERIFIED")
						.requestMatchers("/profile/**").authenticated()
						.requestMatchers(HttpMethod.GET, "/account/email-verification-required", "/account/confirm-action", "/account").authenticated()
						.requestMatchers(HttpMethod.POST, "/account/email-confirmations", "/account/resend-verification").authenticated()
						.requestMatchers(HttpMethod.POST, "/account/email-change-requests").authenticated()
						.requestMatchers(HttpMethod.POST, "/account/email-change-requests/confirm").authenticated()
						.requestMatchers(HttpMethod.POST, "/account/delete-requests").authenticated()
						.requestMatchers(HttpMethod.POST, "/account/delete-requests/confirm").authenticated()
						.requestMatchers("/account/**").hasAuthority("EMAIL_VERIFIED")
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
