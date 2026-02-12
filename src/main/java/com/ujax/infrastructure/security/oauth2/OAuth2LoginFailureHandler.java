package com.ujax.infrastructure.security.oauth2;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

	@Value("${app.ujax.frontend-callback-url}")
	private String frontendCallbackUrl;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException exception) throws IOException {
		String errorCode = "oauth_failed";
		String errorMessage = exception.getMessage();

		if (exception instanceof OAuth2AuthenticationException oauthEx) {
			errorCode = oauthEx.getError().getErrorCode();
		}

		String redirectUrl = UriComponentsBuilder.fromUriString(frontendCallbackUrl)
			.queryParam("error", errorCode)
			.queryParam("message", URLEncoder.encode(errorMessage, StandardCharsets.UTF_8))
			.build()
			.toUriString();
		response.sendRedirect(redirectUrl);
	}
}
