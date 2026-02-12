package com.ujax.application.board.dto.response;

import java.time.LocalDateTime;

import com.ujax.domain.board.BoardComment;

public record CommentResponse(
	Long boardCommentId,
	Long boardId,
	String content,
	BoardAuthorResponse author,
	LocalDateTime createdAt
) {

	public static CommentResponse from(BoardComment comment) {
		return new CommentResponse(
			comment.getId(),
			comment.getBoard().getId(),
			comment.getContent(),
			BoardAuthorResponse.from(comment.getAuthor()),
			comment.getCreatedAt()
		);
	}
}
