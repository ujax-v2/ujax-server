package com.ujax.infrastructure.web.board.dto.request;

import com.ujax.domain.board.BoardType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBoardRequest(
	@NotNull BoardType type,
	@NotBlank String title,
	@NotBlank String content,
	Boolean pinned
) {
}
