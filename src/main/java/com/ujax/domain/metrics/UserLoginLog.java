package com.ujax.domain.metrics;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "user_login_log",
	indexes = {
		@Index(name = "idx_user_login_log_logged_in_at", columnList = "logged_in_at"),
		@Index(name = "idx_user_login_log_user_logged_in", columnList = "user_id, logged_in_at")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserLoginLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "logged_in_at", nullable = false)
	private LocalDateTime loggedInAt;

	private UserLoginLog(Long userId) {
		this.userId = userId;
		this.loggedInAt = LocalDateTime.now();
	}

	public static UserLoginLog of(Long userId) {
		return new UserLoginLog(userId);
	}
}
