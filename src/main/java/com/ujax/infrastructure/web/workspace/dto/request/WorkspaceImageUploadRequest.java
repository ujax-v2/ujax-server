package com.ujax.infrastructure.web.workspace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkspaceImageUploadRequest(

	@NotBlank
	String contentType,

	@NotNull
	Long fileSize
) {
}
