package com.ujax.infrastructure.web.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignupCompleteRequest(
	@NotBlank String requestToken,
	@NotBlank String code,
	@NotBlank @Email String email,
	@NotBlank String password,
	@NotBlank String name
) {
}
