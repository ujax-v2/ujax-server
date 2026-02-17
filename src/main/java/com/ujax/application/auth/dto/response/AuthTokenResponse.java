package com.ujax.application.auth.dto.response;

public record AuthTokenResponse(
	String accessToken,
	String refreshToken
) {
}
