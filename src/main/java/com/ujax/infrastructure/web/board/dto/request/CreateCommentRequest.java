package com.ujax.infrastructure.web.board.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(
	@NotBlank String content
) {
}
