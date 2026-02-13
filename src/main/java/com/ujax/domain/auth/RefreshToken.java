package com.ujax.domain.auth;

import java.time.LocalDateTime;

import com.ujax.domain.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, unique = true)
	private String tokenHash;

	@Column(nullable = false)
	private LocalDateTime expiresAt;

	/** null이면 유효, 값이 있으면 해지됨 */
	private LocalDateTime revokedAt;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	private RefreshToken(User user, String tokenHash, LocalDateTime expiresAt) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
		this.createdAt = LocalDateTime.now();
	}

	public static RefreshToken create(User user, String tokenHash, LocalDateTime expiresAt) {
		return new RefreshToken(user, tokenHash, expiresAt);
	}

	public boolean isExpired() {
		return LocalDateTime.now().isAfter(expiresAt);
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public void revoke() {
		this.revokedAt = LocalDateTime.now();
	}
}
