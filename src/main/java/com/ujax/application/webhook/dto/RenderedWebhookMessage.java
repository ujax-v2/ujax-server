package com.ujax.application.webhook.dto;

import java.time.LocalDateTime;

public record RenderedWebhookMessage(
	Long workspaceProblemId,
	Long workspaceId,
	String workspaceName,
	String problemTitle,
	LocalDateTime deadline,
	LocalDateTime scheduledAt,
	String problemLink,
	String title,
	String body,
	String footer
) {
}
