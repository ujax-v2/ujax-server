package com.ujax.application.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.domain.auth.RefreshToken;
import com.ujax.domain.auth.RefreshTokenRepository;
import com.ujax.domain.user.User;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.UnauthorizedException;
import com.ujax.infrastructure.security.jwt.JwtProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtProperties jwtProperties;
	private final SecureRandom secureRandom = new SecureRandom();

	@Transactional
	public String issue(User user) {
		refreshTokenRepository.revokeAllByUserId(user.getId(), LocalDateTime.now());

		String rawToken = generateRawToken();
		String tokenHash = hashToken(rawToken);
		LocalDateTime expiresAt = LocalDateTime.now()
			.plusSeconds(jwtProperties.refreshExpiration() / 1000);

		RefreshToken refreshToken = RefreshToken.create(user, tokenHash, expiresAt);
		refreshTokenRepository.save(refreshToken);

		return rawToken;
	}

	public User validate(String rawToken) {
		String tokenHash = hashToken(rawToken);
		RefreshToken refreshToken = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash)
			.orElseThrow(() -> new UnauthorizedException(ErrorCode.INVALID_TOKEN));

		if (refreshToken.isExpired()) {
			throw new UnauthorizedException(ErrorCode.EXPIRED_TOKEN);
		}

		return refreshToken.getUser();
	}

	@Transactional
	public void revoke(String rawToken) {
		String tokenHash = hashToken(rawToken);
		refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash)
			.ifPresent(RefreshToken::revoke);
	}

	private String generateRawToken() {
		byte[] bytes = new byte[48];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hashToken(String rawToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm not available", e);
		}
	}
}
