package com.ujax.application.board.dto.response;

import com.ujax.domain.workspace.WorkspaceMember;

public record BoardAuthorResponse(
	Long workspaceMemberId,
	String nickname
) {

	public static BoardAuthorResponse from(WorkspaceMember member) {
		return new BoardAuthorResponse(member.getId(), member.getNickname());
	}
}
