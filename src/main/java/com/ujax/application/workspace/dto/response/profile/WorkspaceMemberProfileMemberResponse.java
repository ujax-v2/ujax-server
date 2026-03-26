package com.ujax.application.workspace.dto.response.profile;

import java.time.LocalDate;

import com.ujax.domain.workspace.WorkspaceMember;
import com.ujax.domain.workspace.WorkspaceMemberRole;

public record WorkspaceMemberProfileMemberResponse(
	Long workspaceMemberId,
	String nickname,
	String email,
	String profileImageUrl,
	String baekjoonId,
	WorkspaceMemberRole role,
	LocalDate joinedAt
) {

	public static WorkspaceMemberProfileMemberResponse from(WorkspaceMember member) {
		return new WorkspaceMemberProfileMemberResponse(
			member.getId(),
			member.getNickname(),
			member.getUser().getEmail(),
			member.getUser().getProfileImageUrl(),
			member.getUser().getBaekjoonId(),
			member.getRole(),
			member.getCreatedAt().toLocalDate()
		);
	}
}
