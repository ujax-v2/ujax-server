package com.ujax.domain.board;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BoardType {
	FREE("자유"),
	NOTICE("공지"),
	QNA("질문"),
	DATA("자료");

	private final String description;
}
