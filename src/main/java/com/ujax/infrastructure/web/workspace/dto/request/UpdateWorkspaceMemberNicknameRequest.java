package com.ujax.infrastructure.web.workspace.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateWorkspaceMemberNicknameRequest(
	@NotBlank String nickname
) {
}
