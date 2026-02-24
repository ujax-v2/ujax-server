package com.ujax.infrastructure.external.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.s3")
public record S3Properties(
	String bucket,
	String region,
	String accessKey,
	String secretKey,
	long presignedUrlExpiry
) {
}
