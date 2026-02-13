package com.ujax.domain.user;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * User 엔티티 단위 테스트
 */
class UserTest {

	@Nested
	@DisplayName("OAuth 유저 생성")
	class CreateOAuthUser {

		@Test
		@DisplayName("OAuth 유저를 생성한다")
		void createOAuthUser() {
			// given
			String email = "test@gmail.com";
			String name = "테스트유저";
			String profileImageUrl = "https://example.com/profile.jpg";
			String providerId = "google-123456";

			// when
			User user = User.createOAuthUser(email, name, profileImageUrl, AuthProvider.GOOGLE, providerId);

			// then
			assertThat(user).extracting("email", "name", "profileImageUrl", "provider", "providerId")
				.containsExactly(email, name, profileImageUrl, AuthProvider.GOOGLE, providerId);
			assertThat(user.getPassword()).isNull();
		}

		@Test
		@DisplayName("프로필 이미지가 없으면 기본 이미지가 설정된다")
		void createOAuthUser_DefaultProfileImage() {
			// when
			User user = User.createOAuthUser("test@gmail.com", "테스트유저", null, AuthProvider.GOOGLE, "google-123");

			// then
			assertThat(user.getProfileImageUrl()).isNotNull();
		}
	}

	@Nested
	@DisplayName("로컬 유저 생성")
	class CreateLocalUser {

		@Test
		@DisplayName("이메일/비밀번호로 로컬 유저를 생성한다")
		void createLocalUser() {
			// given
			String email = "local@example.com";
			String password = "password123!";
			String name = "로컬유저";

			// when
			User user = User.createLocalUser(email, Password.ofEncoded(password), name);

			// then
			assertThat(user).extracting("email", "name", "provider", "providerId")
				.containsExactly(email, name, AuthProvider.LOCAL, null);
			assertThat(user.getPassword().getEncodedValue()).isEqualTo(password);
			assertThat(user.getProfileImageUrl()).isNotNull();
		}
	}

	@Nested
	@DisplayName("프로필 수정")
	class UpdateProfile {

		@Test
		@DisplayName("이름과 프로필 이미지를 수정한다")
		void updateNameAndProfileImage() {
			// given
			User user = User.createOAuthUser(
				"test@example.com",
				"원래이름",
				"https://old-image.com/profile.jpg",
				AuthProvider.GOOGLE,
				"google-123"
			);

			// when
			user.updateProfile("새이름", "https://new-image.com/profile.jpg");

			// then
			assertThat(user).extracting("name", "profileImageUrl")
				.containsExactly("새이름", "https://new-image.com/profile.jpg");
		}

		@Test
		@DisplayName("이름만 수정하면 프로필 이미지는 유지된다")
		void updateOnlyName() {
			// given
			String originalImageUrl = "https://original-image.com/profile.jpg";
			User user = User.createOAuthUser(
				"test@example.com",
				"원래이름",
				originalImageUrl,
				AuthProvider.GOOGLE,
				"google-123"
			);

			// when
			user.updateProfile("새이름", null);

			// then
			assertThat(user).extracting("name", "profileImageUrl")
				.containsExactly("새이름", originalImageUrl);
		}

		@Test
		@DisplayName("프로필 이미지만 수정하면 이름은 유지된다")
		void updateOnlyProfileImage() {
			// given
			String originalName = "원래이름";
			User user = User.createOAuthUser(
				"test@example.com",
				originalName,
				"https://old-image.com/profile.jpg",
				AuthProvider.GOOGLE,
				"google-123"
			);

			// when
			user.updateProfile(null, "https://new-image.com/profile.jpg");

			// then
			assertThat(user).extracting("name", "profileImageUrl")
				.containsExactly(originalName, "https://new-image.com/profile.jpg");
		}
	}

	@Nested
	@DisplayName("프로필 이미지 수정")
	class UpdateProfileImage {

		@Test
		@DisplayName("프로필 이미지 URL을 수정한다")
		void updateProfileImage() {
			// given
			User user = User.createOAuthUser(
				"test@example.com",
				"테스트유저",
				"https://old-image.com/profile.jpg",
				AuthProvider.GOOGLE,
				"google-123"
			);
			String newImageUrl = "https://new-image.com/profile.jpg";

			// when
			user.updateProfileImage(newImageUrl);

			// then
			assertThat(user.getProfileImageUrl()).isEqualTo(newImageUrl);
		}
	}
}
