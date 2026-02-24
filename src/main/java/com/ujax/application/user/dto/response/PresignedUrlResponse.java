package com.ujax.application.user.dto.response;

public record PresignedUrlResponse(
	String presignedUrl,
	String imageUrl
) {
}
