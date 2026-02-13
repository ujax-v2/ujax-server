package com.ujax.domain.auth;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ujax.domain.user.AuthProvider;
import com.ujax.domain.user.Password;
import com.ujax.domain.user.User;

class RefreshTokenTest {

	private User createUser() {
		return User.createLocalUser("test@example.com", Password.ofEncoded("password"), "테스트");
	}

	@Test
	@DisplayName("리프레시 토큰을 생성한다")
	void create() {
		// given
		User user = createUser();
		String tokenHash = "hashedToken123";
		LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);

		// when
		RefreshToken refreshToken = RefreshToken.create(user, tokenHash, expiresAt);

		// then
		assertThat(refreshToken).extracting("tokenHash", "revokedAt")
			.containsExactly(tokenHash, null);
		assertThat(refreshToken.getUser()).isEqualTo(user);
		assertThat(refreshToken.isExpired()).isFalse();
		assertThat(refreshToken.isRevoked()).isFalse();
	}

	@Nested
	@DisplayName("토큰 만료 확인")
	class IsExpired {

		@Test
		@DisplayName("만료된 토큰은 true를 반환한다")
		void expired() {
			// given
			RefreshToken refreshToken = RefreshToken.create(
				createUser(), "hash", LocalDateTime.now().minusDays(1)
			);

			// when & then
			assertThat(refreshToken.isExpired()).isTrue();
		}

		@Test
		@DisplayName("유효한 토큰은 false를 반환한다")
		void notExpired() {
			// given
			RefreshToken refreshToken = RefreshToken.create(
				createUser(), "hash", LocalDateTime.now().plusDays(30)
			);

			// when & then
			assertThat(refreshToken.isExpired()).isFalse();
		}
	}

	@Test
	@DisplayName("토큰을 해지한다")
	void revoke() {
		// given
		RefreshToken refreshToken = RefreshToken.create(
			createUser(), "hash", LocalDateTime.now().plusDays(30)
		);

		// when
		refreshToken.revoke();

		// then
		assertThat(refreshToken.isRevoked()).isTrue();
		assertThat(refreshToken.getRevokedAt()).isNotNull();
	}
}
