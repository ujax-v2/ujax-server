package com.ujax.application.workspace.dto.response.profile;

public record WorkspaceMemberProfileAlgorithmStatResponse(
	String name,
	long count,
	int ratio
) {

	public static WorkspaceMemberProfileAlgorithmStatResponse of(String name, long count, long total) {
		int ratio = total == 0 ? 0 : (int)Math.round((count * 100.0) / total);
		return new WorkspaceMemberProfileAlgorithmStatResponse(name, count, ratio);
	}
}
