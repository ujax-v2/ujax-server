package com.ujax.application.workspace.dto.response.profile;

import java.util.List;

public record WorkspaceMemberProfileResponse(
	WorkspaceMemberProfileMemberResponse member,
	WorkspaceMemberProfileSummaryResponse summary,
	WorkspaceMemberProfileAccuracyResponse accuracy,
	List<WorkspaceMemberProfileAlgorithmStatResponse> algorithmStats,
	List<WorkspaceMemberProfileLanguageStatResponse> languageStats
) {

	public static WorkspaceMemberProfileResponse of(
		WorkspaceMemberProfileMemberResponse member,
		WorkspaceMemberProfileSummaryResponse summary,
		WorkspaceMemberProfileAccuracyResponse accuracy,
		List<WorkspaceMemberProfileAlgorithmStatResponse> algorithmStats,
		List<WorkspaceMemberProfileLanguageStatResponse> languageStats
	) {
		return new WorkspaceMemberProfileResponse(member, summary, accuracy, algorithmStats, languageStats);
	}
}
