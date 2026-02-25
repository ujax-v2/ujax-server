package com.ujax.infrastructure.web.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProfileImageUploadRequest(

	@NotBlank
	String contentType,

	@NotNull
	Long fileSize
) {
}
