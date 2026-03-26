package com.ujax.application.workspace.dto.response.profile;

public record WorkspaceMemberProfileAccuracyResponse(
	int rate,
	long acceptedCount,
	long totalCount
) {

	public static WorkspaceMemberProfileAccuracyResponse of(long acceptedCount, long totalCount) {
		int rate = totalCount == 0 ? 0 : (int)Math.round((acceptedCount * 100.0) / totalCount);
		return new WorkspaceMemberProfileAccuracyResponse(rate, acceptedCount, totalCount);
	}
}
