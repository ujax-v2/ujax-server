package com.ujax.application.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.auth.dto.response.AuthTokenResponse;
import com.ujax.domain.user.AuthProvider;
import com.ujax.domain.user.Password;
import com.ujax.domain.user.User;
import com.ujax.domain.user.UserRepository;
import com.ujax.global.exception.ErrorCode;
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

	public void checkEmailAvailability(String email) {
		if (userRepository.existsByEmail(email)) {
			throw new ConflictException(ErrorCode.DUPLICATE_EMAIL);
		}
	}

	@Transactional
	public AuthTokenResponse signup(String email, String password, String name) {
		if (userRepository.existsByEmail(email)) {
			throw new ConflictException(ErrorCode.DUPLICATE_EMAIL);
		}

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

		return issueTokens(user);
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
		return issueTokens(user);
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

	private AuthTokenResponse issueTokens(User user) {
		String accessToken = jwtTokenProvider.createAccessToken(
			user.getId(), user.getRole(), user.getName(), user.getEmail()
		);
		String rawRefreshToken = refreshTokenService.issue(user);
		return new AuthTokenResponse(accessToken, rawRefreshToken);
	}
}
