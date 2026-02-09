package com.ujax.infrastructure.web.workspace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateWorkspaceRequest(
	@NotBlank String name,
	String description,
	@NotNull Long userId
) {
}
