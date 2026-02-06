package com.ujax.domain.workspace;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkspaceMemberRole {

	OWNER("소유자"),
	MANAGER("운영자"),
	MEMBER("멤버")

	;

	private final String text;
}
