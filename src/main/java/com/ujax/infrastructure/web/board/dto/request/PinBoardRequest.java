package com.ujax.infrastructure.web.board.dto.request;

import jakarta.validation.constraints.NotNull;

public record PinBoardRequest(
	@NotNull Boolean pinned
) {
}
