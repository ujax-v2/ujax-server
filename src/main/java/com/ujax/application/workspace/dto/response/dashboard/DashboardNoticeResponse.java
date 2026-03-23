package com.ujax.application.workspace.dto.response.dashboard;

import java.time.LocalDateTime;

import com.ujax.domain.board.Board;

public record DashboardNoticeResponse(
	Long boardId,
	String title,
	String authorNickname,
	LocalDateTime createdAt,
	boolean pinned
) {

	public static DashboardNoticeResponse from(Board board) {
		return new DashboardNoticeResponse(
			board.getId(),
			board.getTitle(),
			board.getAuthor().getNickname(),
			board.getCreatedAt(),
			board.isPinned()
		);
	}
}
