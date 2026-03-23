package com.ujax.application.workspace.dto.response.dashboard;

public record DashboardSummaryResponse(
	long weeklySubmissionCount,
	DashboardHotProblemResponse hotProblem
) {

	public static DashboardSummaryResponse of(long weeklySubmissionCount, DashboardHotProblemResponse hotProblem) {
		return new DashboardSummaryResponse(weeklySubmissionCount, hotProblem);
	}
}
