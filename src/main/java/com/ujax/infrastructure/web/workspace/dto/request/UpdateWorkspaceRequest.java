package com.ujax.infrastructure.web.workspace.dto.request;

public record UpdateWorkspaceRequest(
	String name,
	String description,
	String mmWebhookUrl
) {
}
