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
public class BoardUpdateRequest {

	private final BoardType type;
	private final String title;
	private final String content;
	private final Boolean pinned;
}
