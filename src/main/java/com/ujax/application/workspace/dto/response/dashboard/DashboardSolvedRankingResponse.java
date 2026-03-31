package com.ujax.application.workspace.dto.response.dashboard;

import com.ujax.domain.workspace.WorkspaceMember;

public record DashboardSolvedRankingResponse(
	Long workspaceMemberId,
	String nickname,
	String userImage,
	long solvedCount
) {

	public static DashboardSolvedRankingResponse from(WorkspaceMember member, long solvedCount) {
		return new DashboardSolvedRankingResponse(
			member.getId(),
			member.getNickname(),
			member.getUser().getProfileImageUrl(),
			solvedCount
		);
	}
}
