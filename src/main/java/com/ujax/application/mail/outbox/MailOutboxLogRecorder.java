package com.ujax.application.mail.outbox;

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

	public void record(
		MailOutbox outbox,
		MailOutboxLogEventType eventType,
		MailOutboxStatus fromStatus,
		MailOutboxStatus toStatus
	) {
		MailOutboxLog log = MailOutboxLog.fromOutbox(outbox, eventType, fromStatus, toStatus);
		mailOutboxLogRepository.save(log);
	}
}
