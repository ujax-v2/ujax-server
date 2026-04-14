package com.ujax.infrastructure.external.webhook;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MattermostWebhookPayload(
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

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Attachment(
		String color,
		String title,
		String title_link,
		String text,
		List<Field> fields,
		String footer
	) {
	}

	public record Field(
		String title,
		String value,
		@JsonProperty("short") boolean isShort
	) {
	}
}
