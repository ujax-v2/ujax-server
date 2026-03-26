package com.ujax.application.workspace.dto.response.profile;

public record WorkspaceMemberProfileLanguageStatResponse(
	String name,
	long count,
	int ratio
) {

	public static WorkspaceMemberProfileLanguageStatResponse of(String name, long count, long total) {
		int ratio = total == 0 ? 0 : (int)Math.round((count * 100.0) / total);
		return new WorkspaceMemberProfileLanguageStatResponse(name, count, ratio);
	}
}
