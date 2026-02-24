package com.ujax.infrastructure.external.s3;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

	private final S3Properties s3Properties;

	@Bean
	public S3Client s3Client() {
		return S3Client.builder()
			.region(Region.of(s3Properties.region()))
			.credentialsProvider(credentialsProvider())
			.build();
	}

	@Bean
	public S3Presigner s3Presigner() {
		return S3Presigner.builder()
			.region(Region.of(s3Properties.region()))
			.credentialsProvider(credentialsProvider())
			.build();
	}

	private StaticCredentialsProvider credentialsProvider() {
		String accessKey = s3Properties.accessKey();
		String secretKey = s3Properties.secretKey();
		log.info("[S3] accessKey={}, secretKey={}...{} (length={})",
			accessKey,
			secretKey.substring(0, 4),
			secretKey.substring(secretKey.length() - 4),
			secretKey.length());
		return StaticCredentialsProvider.create(
			AwsBasicCredentials.create(accessKey, secretKey)
		);
	}
}
