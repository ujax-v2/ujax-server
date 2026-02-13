package com.ujax.application.auth;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ujax.application.auth.dto.response.AuthTokenResponse;
import com.ujax.domain.auth.RefreshTokenRepository;
import com.ujax.domain.user.AuthProvider;
import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.global.exception.common.BadRequestException;
import com.ujax.global.exception.common.ConflictException;
import com.ujax.global.exception.common.UnauthorizedException;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@BeforeEach
	void setUp() {
		refreshTokenRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Nested
	@DisplayName("회원가입")
	class Signup {

		@Test
		@DisplayName("새 사용자를 등록하고 토큰을 반환한다")
		void signup_Success() {
			// when
			AuthTokenResponse response = authService.signup("new@example.com", "password123", "새유저");

			// then
			assertThat(response).extracting("accessToken", "refreshToken")
				.doesNotContainNull();
			assertThat(userRepository.findByEmail("new@example.com")).isPresent();
		}

		@Test
		@DisplayName("비밀번호가 규칙에 맞지 않으면 오류가 발생한다")
		void signup_InvalidPassword() {
			// when & then
			assertThatThrownBy(() -> authService.signup("new@example.com", "short1", "유저"))
				.isInstanceOf(BadRequestException.class);
		}

		@Test
		@DisplayName("이미 존재하는 이메일로 가입하면 오류가 발생한다")
		void signup_DuplicateEmail() {
			// given
			authService.signup("existing@example.com", "password123", "기존유저");

			// when & then
			assertThatThrownBy(() -> authService.signup("existing@example.com", "password456", "새유저"))
				.isInstanceOf(ConflictException.class);
		}
	}

	@Nested
	@DisplayName("로그인")
	class Login {

		@Test
		@DisplayName("올바른 자격 증명으로 로그인한다")
		void login_Success() {
			// given
			authService.signup("user@example.com", "password123", "유저");

			// when
			AuthTokenResponse response = authService.login("user@example.com", "password123");

			// then
			assertThat(response).extracting("accessToken", "refreshToken")
				.doesNotContainNull();
		}

		@Test
		@DisplayName("존재하지 않는 이메일로 로그인하면 오류가 발생한다")
		void login_UserNotFound() {
			// when & then
			assertThatThrownBy(() -> authService.login("nonexistent@example.com", "password"))
				.isInstanceOf(UnauthorizedException.class);
		}

		@Test
		@DisplayName("잘못된 비밀번호로 로그인하면 오류가 발생한다")
		void login_WrongPassword() {
			// given
			authService.signup("user@example.com", "password123", "유저");

			// when & then
			assertThatThrownBy(() -> authService.login("user@example.com", "wrongpassword"))
				.isInstanceOf(UnauthorizedException.class);
		}

		@Test
		@DisplayName("OAuth 사용자가 자체 로그인을 시도하면 오류가 발생한다")
		void login_OAuthUser() {
			// given
			User oauthUser = User.createOAuthUser("oauth@example.com", "OAuth유저", null,
				AuthProvider.GOOGLE, "google123");
			userRepository.save(oauthUser);

			// when & then
			assertThatThrownBy(() -> authService.login("oauth@example.com", "password"))
				.isInstanceOf(UnauthorizedException.class);
		}
	}

	@Nested
	@DisplayName("토큰 갱신")
	class Refresh {

		@Test
		@DisplayName("유효한 리프레시 토큰으로 새 토큰을 발급받는다")
		void refresh_Success() {
			// given
			AuthTokenResponse tokens = authService.signup("user@example.com", "password123", "유저");

			// when
			AuthTokenResponse newTokens = authService.refresh(tokens.refreshToken());

			// then
			assertThat(newTokens).extracting("accessToken", "refreshToken")
				.doesNotContainNull();
			assertThat(newTokens.refreshToken()).isNotEqualTo(tokens.refreshToken());
		}

		@Test
		@DisplayName("유효하지 않은 리프레시 토큰으로 갱신하면 오류가 발생한다")
		void refresh_InvalidToken() {
			// when & then
			assertThatThrownBy(() -> authService.refresh("invalidToken"))
				.isInstanceOf(UnauthorizedException.class);
		}
	}

	@Test
	@DisplayName("로그아웃하면 리프레시 토큰이 해지된다")
	void logout() {
		// given
		AuthTokenResponse tokens = authService.signup("user@example.com", "password123", "유저");

		// when
		authService.logout(tokens.refreshToken());

		// then
		assertThatThrownBy(() -> authService.refresh(tokens.refreshToken()))
			.isInstanceOf(UnauthorizedException.class);
	}
}
