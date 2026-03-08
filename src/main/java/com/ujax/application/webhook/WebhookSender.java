package com.ujax.application.webhook;

import java.time.LocalDateTime;

public interface WebhookSender {

	void send(String hookUrl, Long workspaceProblemId, Long workspaceId, LocalDateTime scheduledAt);
}
