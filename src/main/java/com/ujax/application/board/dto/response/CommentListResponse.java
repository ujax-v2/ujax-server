package com.ujax.application.board.dto.response;

import java.util.List;

import com.ujax.global.dto.PageResponse.PageInfo;

public record CommentListResponse(
	List<CommentResponse> items,
	PageInfo page
) {

	public static CommentListResponse of(List<CommentResponse> items, PageInfo pageInfo) {
		return new CommentListResponse(items, pageInfo);
	}
}
