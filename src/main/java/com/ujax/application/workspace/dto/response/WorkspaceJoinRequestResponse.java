package com.ujax.application.workspace.dto.response;

import java.time.LocalDateTime;

import com.ujax.domain.workspace.WorkspaceJoinRequest;
import com.ujax.domain.workspace.WorkspaceJoinRequestStatus;

public record WorkspaceJoinRequestResponse(
	Long requestId,
	Long workspaceId,
	WorkspaceJoinRequestStatus status,
	LocalDateTime createdAt
) {

	public static WorkspaceJoinRequestResponse from(WorkspaceJoinRequest request) {
		return new WorkspaceJoinRequestResponse(
			request.getId(),
			request.getWorkspace().getId(),
			request.getStatus(),
			request.getCreatedAt()
		);
	}
}
