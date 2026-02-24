package com.ujax.infrastructure.external.s3;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.net.URI;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ujax.global.exception.common.BadRequestException;
import com.ujax.infrastructure.external.s3.dto.PresignedUrlResult;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

	@Mock
	private S3Client s3Client;

	@Mock
	private S3Presigner s3Presigner;

	@Mock
	private S3Properties s3Properties;

	@InjectMocks
	private S3StorageService s3StorageService;

	@Nested
	@DisplayName("Presigned URL 생성")
	class GeneratePresignedUrl {

		@Test
		@DisplayName("지원하지 않는 이미지 형식이면 오류가 발생한다")
		void failsWithUnsupportedContentType() {
			// when & then
			assertThatThrownBy(() -> s3StorageService.generatePresignedUrl(1L, "image/gif", 1024))
				.isInstanceOf(BadRequestException.class);
		}

		@Test
		@DisplayName("파일 크기가 0 이하이면 오류가 발생한다")
		void failsWithZeroFileSize() {
			// when & then
			assertThatThrownBy(() -> s3StorageService.generatePresignedUrl(1L, "image/png", 0))
				.isInstanceOf(BadRequestException.class);
		}

		@Test
		@DisplayName("파일 크기가 5MB를 초과하면 오류가 발생한다")
		void failsWithExceededFileSize() {
			// when & then
			long overFiveMb = 5 * 1024 * 1024 + 1;
			assertThatThrownBy(() -> s3StorageService.generatePresignedUrl(1L, "image/png", overFiveMb))
				.isInstanceOf(BadRequestException.class);
		}

		@Test
		@DisplayName("유효한 요청이면 presigned URL을 생성한다")
		void successWithValidRequest() throws Exception {
			// given
			given(s3Properties.bucket()).willReturn("test-bucket");
			given(s3Properties.region()).willReturn("ap-northeast-2");
			given(s3Properties.presignedUrlExpiry()).willReturn(300L);

			PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
			given(presignedRequest.url()).willReturn(URI.create("https://test-bucket.s3.ap-northeast-2.amazonaws.com/presigned").toURL());
			given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presignedRequest);

			// when
			PresignedUrlResult result = s3StorageService.generatePresignedUrl(1L, "image/png", 1024);

			// then
			assertThat(result.presignedUrl()).contains("test-bucket");
			assertThat(result.imageUrl())
				.startsWith("https://test-bucket.s3.ap-northeast-2.amazonaws.com/users/1/profile/")
				.endsWith(".png");
		}
	}

	@Nested
	@DisplayName("S3 이미지 삭제")
	class DeleteByUrl {

		@Test
		@DisplayName("S3 이미지를 삭제한다")
		void deleteSuccess() {
			// given
			given(s3Properties.bucket()).willReturn("test-bucket");
			String imageUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/users/1/profile/uuid.png";

			// when
			s3StorageService.deleteByUrl(imageUrl);

			// then
			then(s3Client).should().deleteObject(any(DeleteObjectRequest.class));
		}

		@Test
		@DisplayName("삭제 실패해도 예외가 전파되지 않는다")
		void deleteFailsGracefully() {
			// given
			given(s3Properties.bucket()).willReturn("test-bucket");
			willThrow(S3Exception.builder().message("error").build())
				.given(s3Client).deleteObject(any(DeleteObjectRequest.class));
			String imageUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/users/1/profile/uuid.png";

			// when & then
			assertThatCode(() -> s3StorageService.deleteByUrl(imageUrl))
				.doesNotThrowAnyException();
		}
	}

}
