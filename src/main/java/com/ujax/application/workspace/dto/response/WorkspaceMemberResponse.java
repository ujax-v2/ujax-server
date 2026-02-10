package com.ujax.application.workspace.dto.response;

import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRole;

public record WorkspaceMemberResponse(
	Long workspaceMemberId,
	String nickname,
	WorkspaceMemberRole role
) {

	public static WorkspaceMemberResponse from(WorkspaceMember member) {
		return new WorkspaceMemberResponse(member.getId(), member.getNickname(), member.getRole());
	}
}
