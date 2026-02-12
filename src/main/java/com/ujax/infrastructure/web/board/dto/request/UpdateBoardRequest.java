package com.ujax.infrastructure.web.board.dto.request;

import com.ujax.domain.board.BoardType;

public record UpdateBoardRequest(
	BoardType type,
	String title,
	String content,
	Boolean pinned
) {
}
