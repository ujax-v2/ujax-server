package com.ujax.application.workspace.dto.response;

import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRole;

public record WorkspaceMemberListResponse(
	Long workspaceMemberId,
	String nickname,
	String email,
	WorkspaceMemberRole role
) {

	public static WorkspaceMemberListResponse from(WorkspaceMember member) {
		return new WorkspaceMemberListResponse(
			member.getId(),
			member.getNickname(),
			member.getUser().getEmail(),
			member.getRole()
		);
	}
}
