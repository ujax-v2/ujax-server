package com.ujax.application.board.dto.response;

import java.time.LocalDateTime;

import com.ujax.domain.board.Board;
import com.ujax.domain.board.BoardType;

public record BoardListItemResponse(
	Long boardId,
	Long workspaceId,
	BoardType type,
	boolean pinned,
	String title,
	String preview,
	long viewCount,
	long likeCount,
	long commentCount,
	boolean myLike,
	BoardAuthorResponse author,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static BoardListItemResponse from(
		Board board,
		String preview,
		long likeCount,
		long commentCount,
		boolean myLike
	) {
		return new BoardListItemResponse(
			board.getId(),
			board.getWorkspace().getId(),
			board.getType(),
			board.isPinned(),
			board.getTitle(),
			preview,
			board.getViewCount(),
			likeCount,
			commentCount,
			myLike,
			BoardAuthorResponse.from(board.getAuthor()),
			board.getCreatedAt(),
			board.getUpdatedAt()
		);
	}
}
