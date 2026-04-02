package com.ujax.application.mail.outbox;

public record WorkspaceInviteMailPayload(
	Long workspaceId,
	String workspaceName
) {
}
