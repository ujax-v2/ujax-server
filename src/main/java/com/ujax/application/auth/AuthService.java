package com.ujax.application.auth;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.auth.dto.response.AuthTokenResponse;
import com.ujax.application.auth.dto.response.SignupStartResponse;

import com.ujax.application.mail.MailNotifier;

import com.ujax.application.metrics.LoginMetricsService;

import com.ujax.domain.auth.PendingSignup;
import com.ujax.domain.auth.PendingSignupRepository;
import com.ujax.domain.auth.VerificationCodeHasher;
import com.ujax.domain.user.AuthProvider;
import com.ujax.domain.user.Password;
import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.BadRequestException;
import com.ujax.global.exception.common.ConflictException;
import com.ujax.global.exception.common.NotFoundException;
import com.ujax.global.exception.common.UnauthorizedException;
import com.ujax.infrastructure.security.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenService refreshTokenService;
	private final PendingSignupRepository pendingSignupRepository;
	private final SignupVerificationCodeGenerator signupVerificationCodeGenerator;
	private final VerificationCodeHasher verificationCodeHasher;
	private final MailNotifier mailNotifier;
	private final SignupVerificationProperties signupVerificationProperties;
	private final LoginMetricsService loginMetricsService;

	public void checkEmailAvailability(String email) {
		ensureEmailAvailable(email);
	}

	@Transactional
	public SignupStartResponse requestSignup(String email) {
		ensureEmailAvailable(email);

		String verificationCode = signupVerificationCodeGenerator.generate();
		String codeHash = verificationCodeHasher.hash(verificationCode);
		LocalDateTime expiresAt = calculateExpiresAt();

		PendingSignup pendingSignup = pendingSignupRepository.findByEmail(email)
				.map(existing -> {
					existing.refreshVerification(codeHash, expiresAt);
					return existing;
				})
				.orElseGet(() -> PendingSignup.create(email, codeHash, expiresAt));

		pendingSignupRepository.save(pendingSignup);
		mailNotifier.enqueueSignupVerification(email, verificationCode, expiresAt);

		return new SignupStartResponse(pendingSignup.getRequestToken(), pendingSignup.getExpiresAt());
	}

	@Transactional
	public AuthTokenResponse completeSignup(String requestToken, String code, String email, String password, String name) {
		PendingSignup pendingSignup = pendingSignupRepository.findByRequestToken(requestToken)
				.orElseThrow(() -> new NotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "회원가입 요청을 찾을 수 없습니다."));

		if (!pendingSignup.getEmail().equals(email)) {
			throw new BadRequestException(ErrorCode.INVALID_INPUT, "인증 요청 이메일과 일치하지 않습니다.");
		}

		pendingSignup.verifyCode(code, verificationCodeHasher);
		ensureEmailAvailable(email);

		User user = User.createLocalUser(
				email,
				Password.encode(password, passwordEncoder),
				name
		);
		userRepository.save(user);
		pendingSignupRepository.deleteByRequestToken(requestToken);

		AuthTokenResponse tokens = issueTokens(user);
		loginMetricsService.recordLogin(user.getId());
		return tokens;
	}

	@Transactional
	public AuthTokenResponse signup(String email, String password, String name) {
		ensureEmailAvailable(email);

		User user = User.createLocalUser(email, Password.encode(password, passwordEncoder), name);
		userRepository.save(user);

		return issueTokens(user);
	}

	@Transactional
	public AuthTokenResponse login(String email, String password) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS));

		if (user.getProvider() != AuthProvider.LOCAL) {
			throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
		}

		if (!user.matchesPassword(password, passwordEncoder)) {
			throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
		}

		AuthTokenResponse tokens = issueTokens(user);
		loginMetricsService.recordLogin(user.getId());
		return tokens;
	}

	@Transactional
	public AuthTokenResponse refresh(String refreshToken) {
		User user = refreshTokenService.validate(refreshToken);
		refreshTokenService.revoke(refreshToken);
		return issueTokens(user);
	}

	@Transactional
	public User findOrCreateOAuthUser(String email, String name, String profileImageUrl,
									  AuthProvider provider, String providerId) {
		return userRepository.findByProviderAndProviderId(provider, providerId)
				.orElseGet(() -> registerOAuthUser(email, name, profileImageUrl, provider, providerId));
	}

	@Transactional
	public AuthTokenResponse oauthLogin(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));
		AuthTokenResponse tokens = issueTokens(user);
		loginMetricsService.recordLogin(user.getId());
		return tokens;
	}

	@Transactional
	public void logout(String refreshToken) {
		refreshTokenService.revoke(refreshToken);
	}

	private User registerOAuthUser(String email, String name, String profileImageUrl,
								   AuthProvider provider, String providerId) {
		if (userRepository.existsByEmail(email)) {
			throw new ConflictException(ErrorCode.OAUTH_ACCOUNT_EXISTS);
		}
		User user = User.createOAuthUser(email, name, profileImageUrl, provider, providerId);
		return userRepository.save(user);
	}

	private void ensureEmailAvailable(String email) {
		if (userRepository.existsByEmail(email)) {
			throw new ConflictException(ErrorCode.DUPLICATE_EMAIL);
		}
	}

	private LocalDateTime calculateExpiresAt() {
		return LocalDateTime.now().plusMinutes(signupVerificationProperties.ttlMinutes());
	}

	private AuthTokenResponse issueTokens(User user) {
		String accessToken = jwtTokenProvider.createAccessToken(
				user.getId(), user.getRole(), user.getName(), user.getEmail()
		);
		String rawRefreshToken = refreshTokenService.issue(user);
		return new AuthTokenResponse(accessToken, rawRefreshToken);
	}
}
