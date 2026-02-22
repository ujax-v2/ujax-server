package com.ujax.application.board.dto.response;

import java.time.LocalDateTime;

import com.ujax.domain.board.Board;
import com.ujax.domain.board.BoardType;

public record BoardDetailResponse(
	Long boardId,
	Long workspaceId,
	BoardType type,
	boolean pinned,
	String title,
	String content,
	long viewCount,
	long likeCount,
	long commentCount,
	boolean myLike,
	BoardAuthorResponse author,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {

	public static BoardDetailResponse from(
		Board board,
		long likeCount,
		long commentCount,
		boolean myLike
	) {
		return from(board, board.getViewCount(), likeCount, commentCount, myLike);
	}

	public static BoardDetailResponse from(
		Board board,
		long viewCount,
		long likeCount,
		long commentCount,
		boolean myLike
	) {
		return new BoardDetailResponse(
			board.getId(),
			board.getWorkspace().getId(),
			board.getType(),
			board.isPinned(),
			board.getTitle(),
			board.getContent(),
			viewCount,
			likeCount,
			commentCount,
			myLike,
			BoardAuthorResponse.from(board.getAuthor()),
			board.getCreatedAt(),
			board.getUpdatedAt()
		);
	}
}
