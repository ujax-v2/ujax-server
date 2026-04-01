package com.ujax.domain.auth;

import java.time.LocalDateTime;
import java.util.UUID;

import com.ujax.domain.common.BaseEntity;
import com.ujax.global.exception.ErrorCode;
import com.ujax.global.exception.common.BadRequestException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "pending_signups")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PendingSignup extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 36)
	private String requestToken;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String codeHash;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	private PendingSignup(String email, String codeHash, LocalDateTime expiresAt) {
		this.requestToken = UUID.randomUUID().toString();
		this.email = email;
		this.codeHash = codeHash;
		this.expiresAt = expiresAt;
	}

	public static PendingSignup create(
		String email,
		String codeHash,
		LocalDateTime expiresAt
	) {
		return new PendingSignup(email, codeHash, expiresAt);
	}

	public void refreshVerification(String newCodeHash, LocalDateTime newExpiresAt) {
		this.codeHash = newCodeHash;
		this.expiresAt = newExpiresAt;
	}

	public void verifyCode(String rawCode, VerificationCodeHasher verificationCodeHasher) {
		if (isExpired()) {
			throw new BadRequestException(ErrorCode.EXPIRED_VERIFICATION_CODE);
		}
		if (!verificationCodeHasher.matches(rawCode, codeHash)) {
			throw new BadRequestException(ErrorCode.INVALID_VERIFICATION_CODE);
		}
	}

	public boolean isExpired() {
		return !expiresAt.isAfter(LocalDateTime.now());
	}
}
