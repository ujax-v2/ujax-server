package com.ujax.application.workspace.dto.response;

import com.ujax.domain.workspace.Workspace;

public record WorkspaceSettingsResponse(
	Long id,
	String name,
	String description,
	String imageUrl,
	String hookUrl
) {

	private static final String HOOK_PATH_MARKER = "/hooks/";
	private static final char MASK_CHAR = '*';

	public static WorkspaceSettingsResponse from(Workspace workspace) {
		return new WorkspaceSettingsResponse(
			workspace.getId(),
			workspace.getName(),
			workspace.getDescription(),
			workspace.getImageUrl(),
			maskHookUrl(workspace.getHookUrl())
		);
	}

	private static String maskHookUrl(String hookUrl) {
		if (hookUrl == null || hookUrl.isBlank()) {
			return hookUrl;
		}

		int maskStartIndex = findMaskStartIndex(hookUrl);
		int maskedLength = hookUrl.length() - maskStartIndex;
		return hookUrl.substring(0, maskStartIndex) + String.valueOf(MASK_CHAR).repeat(maskedLength);
	}

	private static int findMaskStartIndex(String hookUrl) {
		int hookPathIndex = hookUrl.indexOf(HOOK_PATH_MARKER);
		if (hookPathIndex >= 0) {
			return hookPathIndex + HOOK_PATH_MARKER.length();
		}

		int lastSlashIndex = hookUrl.lastIndexOf('/');
		if (lastSlashIndex >= 0 && lastSlashIndex + 1 < hookUrl.length()) {
			return lastSlashIndex + 1;
		}

		return 0;
	}
}
