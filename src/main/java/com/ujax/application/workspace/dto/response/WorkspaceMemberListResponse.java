package com.ujax.application.workspace.dto.response;

import java.util.List;

public record WorkspaceMemberListResponse(List<WorkspaceMemberResponse> items) {

	public static WorkspaceMemberListResponse of(List<WorkspaceMemberResponse> items) {
		return new WorkspaceMemberListResponse(items);
	}
}
