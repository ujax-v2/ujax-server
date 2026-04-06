package com.ujax.domain.mail;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.annotation.CreatedDate;
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
	name = "mail_outbox_log",
	indexes = {
		@Index(name = "idx_mail_outbox_log_outbox_created", columnList = "mail_outbox_id,created_at")
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MailOutboxLog {

	private static final int MAX_ERROR_LENGTH = 500;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "mail_outbox_log_id")
	private Long id;

	@Column(name = "mail_outbox_id", nullable = false)
	private Long mailOutboxId;

	@Enumerated(EnumType.STRING)
	@Column(name = "mail_type", nullable = false, length = 30)
	private MailType mailType;

	@Column(name = "recipient_email", nullable = false)
	private String recipientEmail;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 32)
	private MailOutboxLogEventType eventType;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status", length = 20)
	private MailOutboxLogStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", length = 20)
	private MailOutboxLogStatus toStatus;

	@Column(name = "attempt_no")
	private Integer attemptNo;

	@Column(name = "next_attempt_at")
	private LocalDateTime nextAttemptAt;

	@Column(name = "sent_at")
	private LocalDateTime sentAt;

	@Column(name = "last_error", length = 500)
	private String lastError;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	private MailOutboxLog(
		Long mailOutboxId,
		MailType mailType,
		String recipientEmail,
		MailOutboxLogEventType eventType,
		MailOutboxLogStatus fromStatus,
		MailOutboxLogStatus toStatus,
		Integer attemptNo,
		LocalDateTime nextAttemptAt,
		LocalDateTime sentAt,
		String lastError
	) {
		this.mailOutboxId = Objects.requireNonNull(mailOutboxId);
		this.mailType = Objects.requireNonNull(mailType);
		this.recipientEmail = Objects.requireNonNull(recipientEmail);
		this.eventType = Objects.requireNonNull(eventType);
		this.fromStatus = fromStatus;
		this.toStatus = toStatus;
		this.attemptNo = attemptNo;
		this.nextAttemptAt = nextAttemptAt;
		this.sentAt = sentAt;
		this.lastError = normalizeLastError(lastError);
	}

	public static MailOutboxLog enqueued(MailOutbox outbox) {
		return create(
			outbox,
			MailOutboxLogEventType.ENQUEUED,
			null,
			MailOutboxLogStatus.PENDING,
			outbox.getNextAttemptAt(),
			null,
			outbox.getLastError()
		);
	}

	public static MailOutboxLog transition(
		MailOutbox outbox,
		MailOutboxLogEventType eventType,
		MailOutboxStatus fromStatus,
		MailOutboxStatus toStatus
	) {
		return create(
			outbox,
			eventType,
			MailOutboxLogStatus.from(fromStatus),
			MailOutboxLogStatus.from(toStatus),
			outbox.getNextAttemptAt(),
			null,
			outbox.getLastError()
		);
	}

	public static MailOutboxLog sent(MailOutbox outbox, LocalDateTime sentAt) {
		return create(
			outbox,
			MailOutboxLogEventType.SENT,
			MailOutboxLogStatus.PROCESSING,
			MailOutboxLogStatus.SENT,
			null,
			Objects.requireNonNull(sentAt),
			null
		);
	}

	public static MailOutboxLog failed(MailOutbox outbox, String lastError) {
		return create(
			outbox,
			MailOutboxLogEventType.FAILED,
			MailOutboxLogStatus.PROCESSING,
			MailOutboxLogStatus.FAILED,
			null,
			null,
			lastError
		);
	}

	private static MailOutboxLog create(
		MailOutbox outbox,
		MailOutboxLogEventType eventType,
		MailOutboxLogStatus fromStatus,
		MailOutboxLogStatus toStatus,
		LocalDateTime nextAttemptAt,
		LocalDateTime sentAt,
		String lastError
	) {
		Objects.requireNonNull(outbox);
		return new MailOutboxLog(
			Objects.requireNonNull(outbox.getId()),
			outbox.getMailType(),
			outbox.getRecipientEmail(),
			eventType,
			fromStatus,
			toStatus,
			outbox.getAttemptNo(),
			nextAttemptAt,
			sentAt,
			lastError
		);
	}

	private String normalizeLastError(String lastError) {
		if (lastError == null) {
			return null;
		}
		return lastError.length() <= MAX_ERROR_LENGTH ? lastError : lastError.substring(0, MAX_ERROR_LENGTH);
	}
}
