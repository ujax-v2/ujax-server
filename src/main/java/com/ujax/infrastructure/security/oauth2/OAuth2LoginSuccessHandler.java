package com.ujax.infrastructure.security.oauth2;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.ujax.application.auth.AuthService;
import com.ujax.application.auth.dto.response.AuthTokenResponse;
import com.ujax.infrastructure.security.UserPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

	private final AuthService authService;

	@Value("${app.ujax.frontend-callback-url}")
	private String frontendCallbackUrl;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException {

		UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
		AuthTokenResponse tokens = authService.oauthLogin(principal.getUserId());

		String redirectUrl = UriComponentsBuilder.fromUriString(frontendCallbackUrl)
			.queryParam("accessToken", tokens.accessToken())
			.queryParam("refreshToken", tokens.refreshToken())
			.build()
			.toUriString();

		response.sendRedirect(redirectUrl);
	}
}
