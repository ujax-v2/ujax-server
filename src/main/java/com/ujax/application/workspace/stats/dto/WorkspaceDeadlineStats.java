package com.ujax.application.workspace.stats.dto;

import java.util.Map;

public record WorkspaceDeadlineStats(
	long totalDeadlineProblems,
	Map<Long, WorkspaceDeadlineRateStat> memberStats
) {
}
