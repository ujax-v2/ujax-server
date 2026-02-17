package com.ujax.application.board.dto.response;

public record BoardLikeStatusResponse(
	long likeCount,
	boolean myLike
) {

	public static BoardLikeStatusResponse of(long likeCount, boolean myLike) {
		return new BoardLikeStatusResponse(likeCount, myLike);
	}
}
