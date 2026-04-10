package com.ujax.infrastructure.web.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignupStartRequest(
	@NotBlank @Email String email
) {
}
