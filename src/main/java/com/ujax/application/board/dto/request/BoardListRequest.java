package com.ujax.application.board.dto.request;

import com.ujax.domain.board.BoardType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BoardListRequest {

	private final BoardType type;
	private final String keyword;
	private final int page;
	private final int size;
	private final String sort;
	private final boolean pinnedFirst;
}
