package com.ujax.application.workspace.dto.response.dashboard;

import com.ujax.domain.workspace.WorkspaceMember;

public record DashboardStreakRankingResponse(
	Long workspaceMemberId,
	String nickname,
	String userImage,
	int streakDays
) {

	public static DashboardStreakRankingResponse from(WorkspaceMember member, int streakDays) {
		return new DashboardStreakRankingResponse(
			member.getId(),
			member.getNickname(),
			member.getUser().getProfileImageUrl(),
			streakDays
		);
	}
}
