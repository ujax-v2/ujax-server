package com.ujax.infrastructure.web.solution.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateSolutionCommentRequest(
	@NotBlank String content
) {
}
