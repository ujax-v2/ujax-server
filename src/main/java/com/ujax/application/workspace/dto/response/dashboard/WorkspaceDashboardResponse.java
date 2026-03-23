package com.ujax.application.workspace.dto.response.dashboard;

import java.util.List;

public record WorkspaceDashboardResponse(
	List<DashboardNoticeResponse> recentNotices,
	List<DashboardDeadlineProblemResponse> upcomingDeadlines,
	DashboardSummaryResponse summary,
	DashboardRankingsResponse rankings
) {

	public static WorkspaceDashboardResponse of(
		List<DashboardNoticeResponse> recentNotices,
		List<DashboardDeadlineProblemResponse> upcomingDeadlines,
		DashboardSummaryResponse summary,
		DashboardRankingsResponse rankings
	) {
		return new WorkspaceDashboardResponse(recentNotices, upcomingDeadlines, summary, rankings);
	}
}
