package com.ujax.application.board.dto.response;

import java.util.List;

import com.ujax.global.dto.PageResponse.PageInfo;

public record BoardListResponse(
	List<BoardListItemResponse> items,
	PageInfo page
) {

	public static BoardListResponse of(List<BoardListItemResponse> items, PageInfo pageInfo) {
		return new BoardListResponse(items, pageInfo);
	}
}
