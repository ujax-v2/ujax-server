package com.ujax.application.workspace.dto.response;

public record WorkspaceMyJoinRequestStatusResponse(
	boolean isMember,
	WorkspaceMyJoinRequestStatus joinRequestStatus,
	boolean canApply
) {

	public static WorkspaceMyJoinRequestStatusResponse of(
		boolean isMember,
		WorkspaceMyJoinRequestStatus joinRequestStatus,
		boolean canApply
	) {
		return new WorkspaceMyJoinRequestStatusResponse(isMember, joinRequestStatus, canApply);
	}
}
