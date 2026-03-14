package com.ujax.application.webhook;

import java.time.LocalDateTime;

public record WebhookAlertMessage(
	Long workspaceProblemId,
	Long workspaceId,
	String workspaceName,
	String problemTitle,
	LocalDateTime deadline,
	LocalDateTime scheduledAt,
	String problemLink
) {
}
