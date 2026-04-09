package com.ujax.application.mail.outbox;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.ujax.domain.mail.MailOutbox;
import com.ujax.domain.mail.MailOutboxStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MailOutboxEventLogger {

	private static final String EVENT_ENQUEUED = "ENQUEUED";
	private static final String EVENT_PROCESSING_STARTED = "PROCESSING_STARTED";
	private static final String EVENT_RECOVERED = "RECOVERED";
	private static final String EVENT_SENT = "SENT";
	private static final String EVENT_RETRY_SCHEDULED = "RETRY_SCHEDULED";
	private static final String EVENT_FAILED = "FAILED";

	private static final String LOG_PATTERN =
		"event=mail_outbox eventType={} outboxId={} mailType={} recipientEmail={} fromStatus={} toStatus={} attemptNo={} nextAttemptAt={} sentAt={} lastError={}";

	public void logEnqueued(MailOutbox outbox) {
		log.info(
			LOG_PATTERN,
			EVENT_ENQUEUED,
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

	public void logProcessingStarted(MailOutbox outbox, MailOutboxStatus fromStatus) {
		log.info(
			LOG_PATTERN,
			EVENT_PROCESSING_STARTED,
			outbox.getId(),
			outbox.getMailType(),
			outbox.getRecipientEmail(),
			fromStatus,
			outbox.getStatus(),
			outbox.getAttemptNo(),
			outbox.getNextAttemptAt(),
			null,
			outbox.getLastError()
		);
	}

	public void logRecovered(MailOutbox outbox) {
		log.warn(
			LOG_PATTERN,
			EVENT_RECOVERED,
			outbox.getId(),
			outbox.getMailType(),
			outbox.getRecipientEmail(),
			MailOutboxStatus.PROCESSING,
			outbox.getStatus(),
			outbox.getAttemptNo(),
			outbox.getNextAttemptAt(),
			null,
			outbox.getLastError()
		);
	}

	public void logRetryScheduled(MailOutbox outbox, MailOutboxStatus fromStatus) {
		log.warn(
			LOG_PATTERN,
			EVENT_RETRY_SCHEDULED,
			outbox.getId(),
			outbox.getMailType(),
			outbox.getRecipientEmail(),
			fromStatus,
			outbox.getStatus(),
			outbox.getAttemptNo(),
			outbox.getNextAttemptAt(),
			null,
			outbox.getLastError()
		);
	}

	public void logSent(MailOutbox outbox, LocalDateTime sentAt) {
		log.info(
			LOG_PATTERN,
			EVENT_SENT,
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
			EVENT_FAILED,
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
