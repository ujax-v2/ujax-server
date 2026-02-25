package com.ujax.infrastructure.web.problem.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateProblemBoxRequest(
	@NotBlank String title,
	String description
) {
}
