package com.ujax.application.webhook;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Component
public class UnsupportedWebhookSender implements WebhookSender {

	@Override
	public void send(String hookUrl, Long workspaceProblemId, Long workspaceId, LocalDateTime scheduledAt) {
		throw new UnsupportedOperationException("TODO(issue-23): implement webhook sender");
	}
}
