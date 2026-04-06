package com.ujax.application.mail.outbox;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.ujax.domain.mail.MailOutbox;
import com.ujax.domain.mail.MailOutboxLog;
import com.ujax.domain.mail.MailOutboxLogEventType;
import com.ujax.domain.mail.MailOutboxLogRepository;
import com.ujax.domain.mail.MailOutboxStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MailOutboxLogRecorder {

	private final MailOutboxLogRepository mailOutboxLogRepository;

	public void recordEnqueued(MailOutbox outbox) {
		mailOutboxLogRepository.save(MailOutboxLog.enqueued(outbox));
	}

	public void recordTransition(
		MailOutbox outbox,
		MailOutboxLogEventType eventType,
		MailOutboxStatus fromStatus,
		MailOutboxStatus toStatus
	) {
		mailOutboxLogRepository.save(MailOutboxLog.transition(outbox, eventType, fromStatus, toStatus));
	}

	public void recordSent(MailOutbox outbox, LocalDateTime sentAt) {
		mailOutboxLogRepository.save(MailOutboxLog.sent(outbox, sentAt));
	}

	public void recordFailed(MailOutbox outbox, String lastError) {
		mailOutboxLogRepository.save(MailOutboxLog.failed(outbox, lastError));
	}
}
