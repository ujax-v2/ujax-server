package com.ujax.infrastructure.web.workspace.dto.request;

import com.ujax.domain.workspace.WorkspaceMemberRole;

import jakarta.validation.constraints.NotNull;

public record UpdateWorkspaceMemberRoleRequest(
	@NotNull WorkspaceMemberRole role
) {
}
