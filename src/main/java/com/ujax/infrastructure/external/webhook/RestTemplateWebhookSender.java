package com.ujax.infrastructure.external.webhook;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.ujax.application.webhook.WebhookAlertMessage;
import com.ujax.application.webhook.WebhookSender;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RestTemplateWebhookSender implements WebhookSender {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM월 dd일 HH:mm");
	private static final DateTimeFormatter PAYLOAD_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	private static final MediaType JSON_UTF8 = new MediaType("application", "json", StandardCharsets.UTF_8);
	private static final String FOOTER = "우작스(UJAX)";

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	@Override
	public void send(String hookUrl, WebhookAlertMessage message) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(JSON_UTF8);
		headers.setAcceptCharset(List.of(StandardCharsets.UTF_8));

		WebhookAlertPayload payload = new WebhookAlertPayload(
			null,
			List.of(
				new Attachment(
					"#2563EB",
					"[%s] %s".formatted(message.workspaceName(), message.problemTitle()),
					message.problemLink(),
					"### 문제 풀이 마감까지 얼마 남지 않았습니다.\n\n**마감일**\n\n**%s**".formatted(
						formatDisplayDateTime(message.deadline())
					),
					null,
					FOOTER
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

		HttpEntity<byte[]> requestEntity = new HttpEntity<>(
			writePayload(payload).getBytes(StandardCharsets.UTF_8),
			headers
		);

		restTemplate.postForEntity(hookUrl, requestEntity, Void.class);
	}

	private String writePayload(WebhookAlertPayload payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("failed to serialize webhook payload", exception);
		}
	}

	private String formatDisplayDateTime(LocalDateTime value) {
		if (value == null) {
			return "미설정";
		}
		return value.format(DATE_TIME_FORMATTER);
	}

	private String formatPayloadDateTime(LocalDateTime value) {
		if (value == null) {
			return null;
		}
		return value.format(PAYLOAD_DATE_TIME_FORMATTER);
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private record WebhookAlertPayload(
		String text,
		List<Attachment> attachments,
		Long workspaceProblemId,
		Long workspaceId,
		String workspaceName,
		String problemTitle,
		String deadline,
		String scheduledAt,
		String problemLink
	) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	private record Attachment(
		String color,
		String title,
		String title_link,
		String text,
		List<Field> fields,
		String footer
	) {
	}

	private record Field(
		String title,
		String value,
		@JsonProperty("short") boolean isShort
	) {
	}
}
