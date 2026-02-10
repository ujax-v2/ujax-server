package com.ujax.application.workspace.dto.response;

import com.ujax.domain.workspace.WorkspaceMember;

public record WorkspaceMemberResponse(
	Long workspaceMemberId,
	String nickname
) {

	public static WorkspaceMemberResponse from(WorkspaceMember member) {
		return new WorkspaceMemberResponse(member.getId(), member.getNickname());
	}
}
