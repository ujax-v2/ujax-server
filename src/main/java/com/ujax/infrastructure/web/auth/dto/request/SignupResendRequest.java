package com.ujax.infrastructure.web.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SignupResendRequest(
	@NotBlank String requestToken
) {
}
