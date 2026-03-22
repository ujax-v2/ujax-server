package com.ujax.application.workspace.dto.response;

import java.time.LocalDateTime;

import com.ujax.domain.workspace.WorkspaceJoinRequest;

public record WorkspaceJoinRequestResponse(
	Long requestId,
	Long workspaceId,
	LocalDateTime createdAt
) {

	public static WorkspaceJoinRequestResponse from(WorkspaceJoinRequest request) {
		return new WorkspaceJoinRequestResponse(
			request.getId(),
			request.getWorkspace().getId(),
			request.getCreatedAt()
		);
	}
}
