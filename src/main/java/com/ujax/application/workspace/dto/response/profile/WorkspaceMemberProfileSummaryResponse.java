package com.ujax.application.workspace.dto.response.profile;

public record WorkspaceMemberProfileSummaryResponse(
	long solvedCount,
	int streakDays,
	String mainLanguage,
	String mainAlgorithm
) {

	public static WorkspaceMemberProfileSummaryResponse of(
		long solvedCount,
		int streakDays,
		String mainLanguage,
		String mainAlgorithm
	) {
		return new WorkspaceMemberProfileSummaryResponse(solvedCount, streakDays, mainLanguage, mainAlgorithm);
	}
}
