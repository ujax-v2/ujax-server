package com.ujax.application.webhook;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.ujax.application.webhook.dto.RenderedWebhookMessage;

@Component
public class WebhookAlertTemplateRenderer {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM월 dd일 HH:mm");
	private static final String FOOTER = "우작스(UJAX)";
	private static final String BODY_TEMPLATE = "### 문제 풀이 마감까지 얼마 남지 않았습니다.\n\n**마감일**\n\n**%s**";

	public RenderedWebhookMessage render(WebhookAlertMessage message) {
		return new RenderedWebhookMessage(
			message.workspaceProblemId(),
			message.workspaceId(),
			message.workspaceName(),
			message.problemTitle(),
			message.deadline(),
			message.scheduledAt(),
			message.problemLink(),
			"[%s] %s".formatted(message.workspaceName(), message.problemTitle()),
			BODY_TEMPLATE.formatted(formatDisplayDateTime(message.deadline())),
			FOOTER
		);
	}

	private String formatDisplayDateTime(LocalDateTime value) {
		if (value == null) {
			return "미설정";
		}
		return value.format(DATE_TIME_FORMATTER);
	}
}
