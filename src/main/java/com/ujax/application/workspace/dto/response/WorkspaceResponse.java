package com.ujax.application.workspace.dto.response;

import com.ujax.domain.workspace.Workspace;

public record WorkspaceResponse(
	Long id,
	String name,
	String description
) {

	public static WorkspaceResponse from(Workspace workspace) {
		return new WorkspaceResponse(
			workspace.getId(),
			workspace.getName(),
			workspace.getDescription()
		);
	}
}
