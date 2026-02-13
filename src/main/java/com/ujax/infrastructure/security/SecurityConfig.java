package com.ujax.infrastructure.security;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.global.exception.ErrorCode;
import com.ujax.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.ujax.infrastructure.security.oauth2.CustomOAuth2UserService;
import com.ujax.infrastructure.security.oauth2.OAuth2LoginFailureHandler;
import com.ujax.infrastructure.security.oauth2.OAuth2LoginSuccessHandler;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final CustomOAuth2UserService customOAuth2UserService;
	private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
	private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
	private final ObjectMapper objectMapper;

	@Value("${app.ujax.base-url}")
	private String baseUrl;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/api/v1/auth/**",
					"/oauth2/**",
					"/login/oauth2/**",
					"/api/v1/problems/**",
					"/api/v1/workspaces/explore",
					"/api/v1/workspaces/search",
					"/error"
				).permitAll()
				.requestMatchers("GET", "/api/v1/workspaces/{id}").permitAll()
				.anyRequest().authenticated()
			)
			.oauth2Login(oauth2 -> oauth2
				.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
				.successHandler(oAuth2LoginSuccessHandler)
				.failureHandler(oAuth2LoginFailureHandler)
			)
			.exceptionHandling(exception -> exception
				.authenticationEntryPoint((request, response, authException) ->
					writeErrorResponse(response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED))
				.accessDeniedHandler((request, response, accessDeniedException) ->
					writeErrorResponse(response, HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED))
			)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	private CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of(baseUrl));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}

	private void writeErrorResponse(HttpServletResponse response, HttpStatus status,
		ErrorCode errorCode) throws IOException {
		ProblemDetail problemDetail = ProblemDetail.forStatus(status);
		problemDetail.setType(URI.create("/docs/index.html#error-code-list"));
		problemDetail.setTitle(errorCode.getCode());
		problemDetail.setDetail(errorCode.getDetail());
		problemDetail.setProperty("exception", "SecurityException");
		problemDetail.setProperty("timestamp", LocalDateTime.now());

		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
	}
}
