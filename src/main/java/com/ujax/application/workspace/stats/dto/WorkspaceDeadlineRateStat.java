package com.ujax.application.workspace.stats.dto;

public record WorkspaceDeadlineRateStat(
	long solvedBeforeDeadlineCount,
	long totalDeadlineProblems,
	int ratePercent
) {

	public static WorkspaceDeadlineRateStat of(long solvedBeforeDeadlineCount, long totalDeadlineProblems) {
		return new WorkspaceDeadlineRateStat(
			solvedBeforeDeadlineCount,
			totalDeadlineProblems,
			calculateRatePercent(solvedBeforeDeadlineCount, totalDeadlineProblems)
		);
	}

	public static WorkspaceDeadlineRateStat empty(long totalDeadlineProblems) {
		return of(0L, totalDeadlineProblems);
	}

	private static int calculateRatePercent(long solvedCount, long totalCount) {
		if (totalCount == 0L) {
			return 0;
		}
		return (int)Math.round((double)solvedCount * 100 / totalCount);
	}
}
