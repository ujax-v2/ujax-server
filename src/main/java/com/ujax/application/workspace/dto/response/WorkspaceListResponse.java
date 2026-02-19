package com.ujax.application.workspace.dto.response;

import java.util.List;

public record WorkspaceListResponse(List<WorkspaceResponse> items) {

	public static WorkspaceListResponse of(List<WorkspaceResponse> items) {
		return new WorkspaceListResponse(items);
	}
}
