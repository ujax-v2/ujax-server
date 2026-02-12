package com.ujax.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SwaggerSecurityConfig {

	@Bean
	@Order(0)
	public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
		http
			.securityMatcher(
				"/swagger/**",
				"/swagger-ui/**",
				"/swagger-ui.html",
				"/v3/api-docs/**",
				"/static/**",
				"/docs/**"
			)
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
		return http.build();
	}
}
