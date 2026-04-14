package com.ujax.infrastructure.external.webhook;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ujax.application.webhook.dto.RenderedWebhookMessage;

@Component
public class MattermostWebhookSender {

	private static final DateTimeFormatter PAYLOAD_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	private static final String COLOR = "#2563EB";

	private final RestTemplateWebhookTransport transport;
	private final ObjectMapper objectMapper;

	public MattermostWebhookSender(
		RestTemplateWebhookTransport transport,
		ObjectMapper objectMapper
	) {
		this.transport = transport;
		this.objectMapper = objectMapper;
	}

	public void send(String hookUrl, RenderedWebhookMessage message) {
		MattermostWebhookPayload payload = new MattermostWebhookPayload(
			null,
			List.of(
				new MattermostWebhookPayload.Attachment(
					COLOR,
					message.title(),
					message.problemLink(),
					message.body(),
					null,
					message.footer()
				)
			),
			message.workspaceProblemId(),
			message.workspaceId(),
			message.workspaceName(),
			message.problemTitle(),
			formatPayloadDateTime(message.deadline()),
			formatPayloadDateTime(message.scheduledAt()),
			message.problemLink()
		);

		transport.sendJson(hookUrl, writePayload(payload));
	}

	private byte[] writePayload(MattermostWebhookPayload payload) {
		try {
			return objectMapper.writeValueAsBytes(payload);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("failed to serialize webhook payload", exception);
		}
	}

	private String formatPayloadDateTime(LocalDateTime value) {
		if (value == null) {
			return null;
		}
		return value.format(PAYLOAD_DATE_TIME_FORMATTER);
	}
}
