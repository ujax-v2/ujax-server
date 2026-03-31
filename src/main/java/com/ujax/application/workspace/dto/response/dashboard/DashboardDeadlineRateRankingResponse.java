package com.ujax.application.workspace.dto.response.dashboard;

import com.ujax.application.workspace.stats.dto.WorkspaceDeadlineRateStat;
import com.ujax.domain.workspace.WorkspaceMember;

public record DashboardDeadlineRateRankingResponse(
	Long workspaceMemberId,
	String nickname,
	String userImage,
	long solvedBeforeDeadlineCount,
	long totalDeadlineProblems,
	int ratePercent
) {

	public static DashboardDeadlineRateRankingResponse from(WorkspaceMember member, WorkspaceDeadlineRateStat stat) {
		return new DashboardDeadlineRateRankingResponse(
			member.getId(),
			member.getNickname(),
			member.getUser().getProfileImageUrl(),
			stat.solvedBeforeDeadlineCount(),
			stat.totalDeadlineProblems(),
			stat.ratePercent()
		);
	}
}
