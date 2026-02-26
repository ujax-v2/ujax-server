package com.ujax.application.workspace.dto.response;

import com.ujax.domain.workspace.Workspace;

public record WorkspaceSettingsResponse(
	Long id,
	String name,
	String description,
	String imageUrl,
	String mmWebhookUrl
) {

	public static WorkspaceSettingsResponse from(Workspace workspace) {
		return new WorkspaceSettingsResponse(
			workspace.getId(),
			workspace.getName(),
			workspace.getDescription(),
			workspace.getImageUrl(),
			workspace.getMmWebhookUrl()
		);
	}
}
