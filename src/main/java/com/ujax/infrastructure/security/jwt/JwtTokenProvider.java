package com.ujax.infrastructure.security.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.ujax.domain.user.UserRole;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.UnauthorizedException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtTokenProvider {

	private final SecretKey secretKey;
	private final long accessExpiration;

	public JwtTokenProvider(JwtProperties jwtProperties) {
		this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
		this.accessExpiration = jwtProperties.accessExpiration();
	}

	public String createAccessToken(Long userId, UserRole role, String name, String email) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + accessExpiration);

		return Jwts.builder()
			.subject(String.valueOf(userId))
			.claim("role", role.name())
			.claim("name", name)
			.claim("email", email)
			.issuedAt(now)
			.expiration(expiry)
			.signWith(secretKey)
			.compact();
	}

	public Claims parseToken(String token) {
		try {
			return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
		} catch (ExpiredJwtException e) {
			throw new UnauthorizedException(ErrorCode.EXPIRED_TOKEN);
		} catch (Exception e) {
			throw new UnauthorizedException(ErrorCode.INVALID_TOKEN);
		}
	}
}
