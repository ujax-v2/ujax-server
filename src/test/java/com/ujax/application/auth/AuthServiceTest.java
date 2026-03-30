package com.ujax.application.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ujax.application.auth.dto.response.AuthTokenResponse;
import com.ujax.application.auth.dto.response.SignupStartResponse;
import com.ujax.domain.auth.PendingSignup;
import com.ujax.domain.auth.PendingSignupRepository;
import com.ujax.domain.auth.RefreshTokenRepository;
import com.ujax.domain.auth.VerificationCodeHasher;
import com.ujax.domain.user.AuthProvider;
import com.ujax.domain.user.Password;
import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.global.exception.ErrorCode;
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

	@Autowired
	private PendingSignupRepository pendingSignupRepository;

	@MockitoBean
	private SignupVerificationCodeGenerator signupVerificationCodeGenerator;

	@MockitoBean
	private VerificationCodeHasher verificationCodeHasher;

	@MockitoBean
	private SignupVerificationMailer signupVerificationMailer;

	@BeforeEach
	void setUp() {
		pendingSignupRepository.deleteAll();
		refreshTokenRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Nested
	@DisplayName("이메일 중복 확인")
	class EmailAvailability {

		@Test
		@DisplayName("가입되지 않은 이메일은 사용 가능하다")
		void checkEmailAvailability_Available() {
			assertThatCode(() -> authService.checkEmailAvailability("new@example.com"))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("이미 가입된 이메일은 중복 오류가 발생한다")
		void checkEmailAvailability_Unavailable() {
			authService.signup("existing@example.com", "password123", "기존유저");

			assertThatThrownBy(() -> authService.checkEmailAvailability("existing@example.com"))
				.isInstanceOf(ConflictException.class)
				.extracting("errorCode")
				.isEqualTo(com.ujax.global.exception.ErrorCode.DUPLICATE_EMAIL);
		}
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
	@DisplayName("회원가입 인증 요청")
	class SignupRequestStart {

		@Test
		@DisplayName("회원정보를 대기 상태에 저장하고 메일을 발송한다")
		void requestSignup_Success() {
			given(signupVerificationCodeGenerator.generate()).willReturn("123456");
			given(verificationCodeHasher.hash("123456")).willReturn("hashed-code");

			SignupStartResponse response = authService.requestSignup("new@example.com", "password123", "새유저");

			assertThat(response.email()).isEqualTo("new@example.com");
			assertThat(response.requestToken()).isNotBlank();
			assertThat(response.expiresAt()).isAfter(LocalDateTime.now());
			assertThat(pendingSignupRepository.findByEmail("new@example.com")).isPresent();
			verify(signupVerificationMailer).sendVerificationCode(eq("new@example.com"), eq("123456"), any(LocalDateTime.class));
		}

		@Test
		@DisplayName("이미 존재하는 이메일이면 실패한다")
		void requestSignup_DuplicateEmail() {
			authService.signup("existing@example.com", "password123", "기존유저");

			assertThatThrownBy(() -> authService.requestSignup("existing@example.com", "password123", "새유저"))
				.isInstanceOf(ConflictException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.DUPLICATE_EMAIL);
		}
	}

	@Nested
	@DisplayName("회원가입 인증 확인")
	class SignupConfirm {

		@Test
		@DisplayName("코드가 일치하면 실제 회원을 생성한다")
		void confirmSignup_Success() {
			Password encodedPassword = Password.encode("password123", new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder());
			PendingSignup pendingSignup = pendingSignupRepository.save(
				PendingSignup.create(
					"confirm@example.com",
					encodedPassword.getEncodedValue(),
					"확인유저",
					"hashed-code",
					LocalDateTime.now().plusMinutes(5)
				)
			);
			given(verificationCodeHasher.matches("123456", "hashed-code")).willReturn(true);

			AuthTokenResponse response = authService.confirmSignup(pendingSignup.getRequestToken(), "123456");

			assertThat(response).extracting("accessToken", "refreshToken").doesNotContainNull();
			assertThat(userRepository.findByEmail("confirm@example.com")).isPresent();
			assertThat(pendingSignupRepository.findByRequestToken(pendingSignup.getRequestToken())).isEmpty();
		}

		@Test
		@DisplayName("코드가 일치하지 않으면 실패한다")
		void confirmSignup_InvalidCode() {
			PendingSignup pendingSignup = pendingSignupRepository.save(
				PendingSignup.create(
					"confirm@example.com",
					"encoded-password",
					"확인유저",
					"hashed-code",
					LocalDateTime.now().plusMinutes(5)
				)
			);
			given(verificationCodeHasher.matches("000000", "hashed-code")).willReturn(false);

			assertThatThrownBy(() -> authService.confirmSignup(pendingSignup.getRequestToken(), "000000"))
				.isInstanceOf(BadRequestException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_VERIFICATION_CODE);
		}

		@Test
		@DisplayName("만료된 코드면 실패한다")
		void confirmSignup_ExpiredCode() {
			PendingSignup pendingSignup = pendingSignupRepository.save(
				PendingSignup.create(
					"expired@example.com",
					"encoded-password",
					"만료유저",
					"hashed-code",
					LocalDateTime.now().minusMinutes(1)
				)
			);

			assertThatThrownBy(() -> authService.confirmSignup(pendingSignup.getRequestToken(), "123456"))
				.isInstanceOf(BadRequestException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.EXPIRED_VERIFICATION_CODE);
		}
	}

	@Nested
	@DisplayName("회원가입 인증 재발송")
	class SignupResend {

		@Test
		@DisplayName("기존 대기 상태를 갱신하고 새 코드를 발송한다")
		void resendSignupCode_Success() {
			PendingSignup pendingSignup = pendingSignupRepository.save(
				PendingSignup.create(
					"resend@example.com",
					"encoded-password",
					"재발송유저",
					"old-code",
					LocalDateTime.now().plusMinutes(5)
				)
			);
			String oldRequestToken = pendingSignup.getRequestToken();
			given(signupVerificationCodeGenerator.generate()).willReturn("654321");
			given(verificationCodeHasher.hash("654321")).willReturn("new-code-hash");

			SignupStartResponse response = authService.resendSignupCode(oldRequestToken);

			assertThat(response.email()).isEqualTo("resend@example.com");
			assertThat(response.requestToken()).isNotEqualTo(oldRequestToken);
			assertThat(pendingSignupRepository.findByRequestToken(response.requestToken())).isPresent();
			assertThat(pendingSignupRepository.findByRequestToken(oldRequestToken)).isEmpty();
			verify(signupVerificationMailer).sendVerificationCode(eq("resend@example.com"), eq("654321"), any(LocalDateTime.class));
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
