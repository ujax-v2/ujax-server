package com.ujax.application.mail.outbox;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.ujax.domain.mail.MailOutbox;
import com.ujax.domain.mail.MailOutboxEventType;
import com.ujax.domain.mail.MailOutboxStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MailOutboxEventLogger {

	private static final String LOG_PATTERN =
		"event=mail_outbox eventType={} outboxId={} mailType={} recipientEmail={} fromStatus={} toStatus={} attemptNo={} nextAttemptAt={} sentAt={} lastError={}";

	public void logEnqueued(MailOutbox outbox) {
		log.info(
			LOG_PATTERN,
			MailOutboxEventType.ENQUEUED,
			outbox.getId(),
			outbox.getMailType(),
			outbox.getRecipientEmail(),
			null,
			MailOutboxStatus.PENDING,
			outbox.getAttemptNo(),
			outbox.getNextAttemptAt(),
			null,
			outbox.getLastError()
		);
	}

	public void logTransition(
		MailOutbox outbox,
		MailOutboxEventType eventType,
		MailOutboxStatus fromStatus,
		MailOutboxStatus toStatus
	) {
		if (eventType == MailOutboxEventType.RECOVERED || eventType == MailOutboxEventType.RETRY_SCHEDULED) {
			log.warn(
				LOG_PATTERN,
				eventType,
				outbox.getId(),
				outbox.getMailType(),
				outbox.getRecipientEmail(),
				fromStatus,
				toStatus,
				outbox.getAttemptNo(),
				outbox.getNextAttemptAt(),
				null,
				outbox.getLastError()
			);
			return;
		}
		log.info(
			LOG_PATTERN,
			eventType,
			outbox.getId(),
			outbox.getMailType(),
			outbox.getRecipientEmail(),
			fromStatus,
			toStatus,
			outbox.getAttemptNo(),
			outbox.getNextAttemptAt(),
			null,
			outbox.getLastError()
		);
	}

	public void logSent(MailOutbox outbox, LocalDateTime sentAt) {
		log.info(
			LOG_PATTERN,
			MailOutboxEventType.SENT,
			outbox.getId(),
			outbox.getMailType(),
			outbox.getRecipientEmail(),
			MailOutboxStatus.PROCESSING,
			"SENT",
			outbox.getAttemptNo(),
			null,
			sentAt,
			null
		);
	}

	public void logFailed(MailOutbox outbox, String lastError) {
		log.warn(
			LOG_PATTERN,
			MailOutboxEventType.FAILED,
			outbox.getId(),
			outbox.getMailType(),
			outbox.getRecipientEmail(),
			MailOutboxStatus.PROCESSING,
			"FAILED",
			outbox.getAttemptNo(),
			null,
			null,
			lastError
		);
	}
}
