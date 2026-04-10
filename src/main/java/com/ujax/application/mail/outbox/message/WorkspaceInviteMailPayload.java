package com.ujax.application.mail.outbox.message;

public record WorkspaceInviteMailPayload(
	Long workspaceId,
	String workspaceName
) {
}
