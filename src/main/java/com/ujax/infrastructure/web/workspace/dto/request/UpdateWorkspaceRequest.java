package com.ujax.infrastructure.web.workspace.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateWorkspaceRequest(
	@NotBlank String name,
	String description,
	String mmWebhookUrl
) {
}
