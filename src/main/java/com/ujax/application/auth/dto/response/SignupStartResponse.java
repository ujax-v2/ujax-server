package com.ujax.application.auth.dto.response;

import java.time.LocalDateTime;

public record SignupStartResponse(
	String requestToken,
	String email,
	LocalDateTime expiresAt
) {
}
