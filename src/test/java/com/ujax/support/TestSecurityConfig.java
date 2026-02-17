package com.ujax.support;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import com.ujax.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.ujax.infrastructure.security.jwt.JwtProperties;
import com.ujax.infrastructure.security.jwt.JwtTokenProvider;
import com.ujax.infrastructure.security.oauth2.CustomOAuth2UserService;
import com.ujax.infrastructure.security.oauth2.OAuth2LoginFailureHandler;
import com.ujax.infrastructure.security.oauth2.OAuth2LoginSuccessHandler;

@TestConfiguration
public class TestSecurityConfig {

	@Bean
	@Order(0)
	public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
		return http.build();
	}

	@Bean
	public JwtProperties jwtProperties() {
		return new JwtProperties(
			"testSecretKeyThatIsAtLeast256BitsLongForHmacSha256AlgorithmInTestProfile",
			1800000L,
			2592000000L
		);
	}

	@Bean
	public JwtTokenProvider jwtTokenProvider() {
		return new JwtTokenProvider(jwtProperties());
	}

	@Bean
	public JwtAuthenticationFilter jwtAuthenticationFilter() {
		return new JwtAuthenticationFilter(jwtTokenProvider());
	}

	@Bean
	public CustomOAuth2UserService customOAuth2UserService() {
		return Mockito.mock(CustomOAuth2UserService.class);
	}

	@Bean
	public OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler() {
		return Mockito.mock(OAuth2LoginSuccessHandler.class);
	}

	@Bean
	public OAuth2LoginFailureHandler oAuth2LoginFailureHandler() {
		return Mockito.mock(OAuth2LoginFailureHandler.class);
	}
}
