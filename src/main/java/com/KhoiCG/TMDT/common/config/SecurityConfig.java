package com.KhoiCG.TMDT.common.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private JwtFilter jwtFilter;

	@Autowired
	private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@Autowired
	private HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

	@Value("${application.security.cors.allowed-origins:http://localhost:3002,http://localhost:3003}")
	private String allowedOrigins;

	@Bean
	public AuthenticationProvider authProvider() {
		DaoAuthenticationProvider provider =
				new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(12);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, CustomOAuth2SuccessHandler customOAuth2SuccessHandler) throws Exception {

		http
//				.csrf(csrf -> csrf.disable())
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(session ->
						session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				)
				.exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/api/auth/register",
								"/api/auth/verify-otp",
								"/api/auth/login",
								"/api/auth/refresh-token",
								"/api/auth/forgot-password",
								"/api/auth/reset-password",
								"/oauth2/**",
								"/api/login/oauth2/**",
								"/api/vnpay/ipn",
								"/api/vnpay/return",
								"/api/webhooks/**",
								"/api/error",
								"/api/chatbot/**",
								"/api/shipping/quote",
								"/api/store/config"
						).permitAll()
						.requestMatchers("/actuator/health").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/info-pages/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/banners/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/reviews").authenticated()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PATCH, "/api/products/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/categories/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/categories/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/banners/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/banners/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/banners/**").hasRole("ADMIN")
						.anyRequest()
//						.permitAll()
						.authenticated()
				).oauth2Login(oauth2 -> oauth2
						.authorizationEndpoint(authorization -> authorization
								.baseUri("/oauth2/authorization")
								.authorizationRequestRepository(cookieAuthorizationRequestRepository)
						)
						.successHandler(customOAuth2SuccessHandler)
						.failureUrl("/login?error=true")
						.failureHandler((request, response, exception) -> {
							response.sendRedirect("/login?error=true");
						})
				)
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}


	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		List<String> origins = Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)
				.filter(s -> !s.isBlank())
				.toList();
		config.setAllowedOrigins(origins);
		config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS","PATCH"));
		config.setAllowedHeaders(List.of(
				"Authorization", "Content-Type", "Accept", "X-Requested-With",
				"Cache-Control", "X-CSRF-Token", "Cookie"
		));
		config.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
