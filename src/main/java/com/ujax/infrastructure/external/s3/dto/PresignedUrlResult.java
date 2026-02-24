package com.ujax.infrastructure.external.s3.dto;

public record PresignedUrlResult(
	String presignedUrl,
	String imageUrl
) {
}
