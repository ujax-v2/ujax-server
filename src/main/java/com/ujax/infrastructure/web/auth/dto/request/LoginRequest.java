package com.ujax.infrastructure.web.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
	@NotBlank String email,
	@NotBlank String password
) {
}
