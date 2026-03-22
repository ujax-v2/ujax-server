package com.ujax.application.workspace.dto.response;

import java.time.LocalDateTime;

import com.ujax.domain.workspace.WorkspaceJoinRequest;

public record WorkspaceJoinRequestListItemResponse(
	Long requestId,
	Long workspaceId,
	Long applicantUserId,
	String applicantName,
	LocalDateTime createdAt
) {

	public static WorkspaceJoinRequestListItemResponse from(WorkspaceJoinRequest request) {
		return new WorkspaceJoinRequestListItemResponse(
			request.getId(),
			request.getWorkspace().getId(),
			request.getUser().getId(),
			request.getUser().getName(),
			request.getCreatedAt()
		);
	}
}
