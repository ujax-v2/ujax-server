package com.ujax.infrastructure.external.s3;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

	private final S3Properties s3Properties;

	@Bean(destroyMethod = "close")
	public S3Client s3Client() {
		return S3Client.builder()
			.region(Region.of(s3Properties.region()))
			.credentialsProvider(credentialsProvider())
			.build();
	}

	@Bean(destroyMethod = "close")
	public S3Presigner s3Presigner() {
		return S3Presigner.builder()
			.region(Region.of(s3Properties.region()))
			.credentialsProvider(credentialsProvider())
			.build();
	}

	private StaticCredentialsProvider credentialsProvider() {
		return StaticCredentialsProvider.create(
			AwsBasicCredentials.create(s3Properties.accessKey(), s3Properties.secretKey())
		);
	}
}
