package com.ujax.infrastructure.web.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SignupConfirmRequest(
	@NotBlank String requestToken,
	@NotBlank String code
) {
}
