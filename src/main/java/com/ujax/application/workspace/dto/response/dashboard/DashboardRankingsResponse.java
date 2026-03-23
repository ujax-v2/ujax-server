package com.ujax.application.workspace.dto.response.dashboard;

import java.util.List;

public record DashboardRankingsResponse(
	List<DashboardSolvedRankingResponse> monthlySolved,
	List<DashboardStreakRankingResponse> streak,
	List<DashboardDeadlineRateRankingResponse> deadlineRate
) {

	public static DashboardRankingsResponse of(
		List<DashboardSolvedRankingResponse> monthlySolved,
		List<DashboardStreakRankingResponse> streak,
		List<DashboardDeadlineRateRankingResponse> deadlineRate
	) {
		return new DashboardRankingsResponse(monthlySolved, streak, deadlineRate);
	}
}
