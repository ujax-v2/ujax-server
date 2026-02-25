package com.ujax.infrastructure.external.s3;

import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.BadRequestException;
import com.ujax.infrastructure.external.s3.dto.PresignedUrlResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {

	//MAX_FILE_SIZE = 5MB
	private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final S3Properties s3Properties;

	public PresignedUrlResult generatePresignedUrl(Long userId, String contentType, long fileSize) {
		validateContentType(contentType);
		validateFileSize(fileSize);

		String key = getKey(userId, contentType);

		String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
			s3Properties.bucket(), s3Properties.region(), key);
		PresignedPutObjectRequest presigned = getPresigned(contentType, fileSize, key);
		return new PresignedUrlResult(presigned.url().toString(), imageUrl);
	}

	public void deleteByUrl(String imageUrl) {
		try {
			String key = URI
				.create(imageUrl)
				.getPath()
				.substring(1);

			s3Client.deleteObject(DeleteObjectRequest.builder()
				.bucket(s3Properties.bucket())
				.key(key)
				.build());
		} catch (Exception e) {
			log.warn("S3 이미지 삭제 실패: url={}", imageUrl, e);
		}
	}

	private String getKey(Long userId, String contentType) {
		String extension = contentType.substring(contentType.indexOf('/') + 1);
		return "users/" + userId + "/profile/" + UUID.randomUUID() + "." + extension;
	}

	private PresignedPutObjectRequest getPresigned(String contentType, long fileSize, String key) {
		return s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
			.signatureDuration(Duration.ofSeconds(s3Properties.presignedUrlExpiry()))
			.putObjectRequest(builder -> builder
				.bucket(s3Properties.bucket())
				.key(key)
				.contentType(contentType)
			)
			.build());
	}

	private void validateContentType(String contentType) {
		if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
			throw new BadRequestException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
		}
	}

	private void validateFileSize(long fileSize) {
		if (fileSize <= 0 || fileSize > MAX_FILE_SIZE) {
			throw new BadRequestException(ErrorCode.IMAGE_SIZE_EXCEEDED);
		}
	}

}
