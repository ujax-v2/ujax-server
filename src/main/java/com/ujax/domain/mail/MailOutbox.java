package com.ujax.domain.mail;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "mail_outbox",
	indexes = {
		@Index(name = "idx_mail_outbox_due", columnList = "status,next_attempt_at"),
		@Index(name = "idx_mail_outbox_status_updated_at", columnList = "status,updated_at")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MailOutbox {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "mail_outbox_id")
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "mail_type", nullable = false, length = 30)
	private MailType mailType;

	@Column(name = "recipient_email", nullable = false)
	private String recipientEmail;

	@Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
	private String payloadJson;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MailOutboxStatus status;

	@Column(name = "attempt_no", nullable = false)
	private int attemptNo;

	@Column(name = "next_attempt_at", nullable = false)
	private LocalDateTime nextAttemptAt;

	@Column(name = "sent_at")
	private LocalDateTime sentAt;

	@Column(name = "last_error", length = 500)
	private String lastError;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	private MailOutbox(MailType mailType, String recipientEmail, String payloadJson, LocalDateTime nextAttemptAt) {
		this.mailType = Objects.requireNonNull(mailType);
		this.recipientEmail = Objects.requireNonNull(recipientEmail);
		this.payloadJson = Objects.requireNonNull(payloadJson);
		this.status = MailOutboxStatus.PENDING;
		this.attemptNo = 0;
		this.nextAttemptAt = Objects.requireNonNull(nextAttemptAt);
	}

	public static MailOutbox create(
		MailType mailType,
		String recipientEmail,
		String payloadJson,
		LocalDateTime nextAttemptAt
	) {
		return new MailOutbox(mailType, recipientEmail, payloadJson, nextAttemptAt);
	}

	public void markProcessing() {
		validateStatus(MailOutboxStatus.PENDING);
		this.status = MailOutboxStatus.PROCESSING;
		this.attemptNo = this.attemptNo + 1;
	}

	public void scheduleRetry(LocalDateTime retryAt, String lastError) {
		validateStatus(MailOutboxStatus.PROCESSING);
		this.status = MailOutboxStatus.PENDING;
		this.nextAttemptAt = Objects.requireNonNull(retryAt);
		this.lastError = lastError;
	}

	public void recoverToPending(LocalDateTime retryAt) {
		validateStatus(MailOutboxStatus.PROCESSING);
		this.status = MailOutboxStatus.PENDING;
		this.nextAttemptAt = Objects.requireNonNull(retryAt);
	}

	public void markSent(LocalDateTime sentAt) {
		validateStatus(MailOutboxStatus.PROCESSING);
		this.status = MailOutboxStatus.SENT;
		this.sentAt = Objects.requireNonNull(sentAt);
		this.nextAttemptAt = sentAt;
		this.lastError = null;
	}

	public void markFailed(String lastError) {
		validateStatus(MailOutboxStatus.PROCESSING);
		this.status = MailOutboxStatus.FAILED;
		this.lastError = lastError;
	}

	public boolean isRetryExhausted(int maxAttempts) {
		if (maxAttempts <= 0) {
			throw new IllegalArgumentException("maxAttempts must be greater than zero");
		}
		return this.attemptNo >= maxAttempts;
	}

	private void validateStatus(MailOutboxStatus expected) {
		if (this.status != expected) {
			throw new IllegalStateException("invalid mail outbox state");
		}
	}
}
