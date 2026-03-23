package com.ujax.infrastructure.web.board.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BoardImageUploadRequest(

	@NotBlank
	String contentType,

	@NotNull
	Long fileSize
) {
}
