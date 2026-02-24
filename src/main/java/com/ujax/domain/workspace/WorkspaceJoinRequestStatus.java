package com.ujax.domain.workspace;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkspaceJoinRequestStatus {

	PENDING("대기"),
	APPROVED("수락"),
	REJECTED("거절")

	;

	private final String text;
}
