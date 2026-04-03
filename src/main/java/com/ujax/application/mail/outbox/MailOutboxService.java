package com.ujax.application.mail.outbox;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.domain.mail.MailOutbox;
import com.ujax.domain.mail.MailOutboxLogEventType;
import com.ujax.domain.mail.MailOutboxRepository;
import com.ujax.domain.mail.MailOutboxStatus;
import com.ujax.domain.mail.MailType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MailOutboxService {

	private static final int MAX_ERROR_LENGTH = 500;

	private final MailOutboxRepository mailOutboxRepository;
	private final List<MailOutboxHandler> handlers;
	private final UjaxSmtpMailSender ujaxSmtpMailSender;
	private final MailOutboxDeliveryProperties properties;
	private final MailOutboxLogRecorder mailOutboxLogRecorder;

	@Transactional
	public void recoverStuckProcessing(LocalDateTime now) {
		List<MailOutbox> stuckOutboxes = mailOutboxRepository.findAllByStatusAndUpdatedAtBefore(
			MailOutboxStatus.PROCESSING,
			now.minusMinutes(properties.stuckProcessingMinutes())
		);

		for (MailOutbox outbox : stuckOutboxes) {
			outbox.recoverToPending(now);
			mailOutboxLogRecorder.record(
				outbox,
				MailOutboxLogEventType.RECOVERED,
				MailOutboxStatus.PROCESSING,
				outbox.getStatus()
			);
		}
	}

	@Transactional
	public List<Long> reserveDueMailOutboxIds(LocalDateTime now, int limit) {
		if (limit <= 0) {
			return List.of();
		}

		List<MailOutbox> dueOutboxes = mailOutboxRepository.findAllByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
			MailOutboxStatus.PENDING,
			now,
			PageRequest.of(0, limit)
		);

		for (MailOutbox outbox : dueOutboxes) {
			MailOutboxStatus fromStatus = outbox.getStatus();
			outbox.markProcessing();
			mailOutboxLogRecorder.record(
				outbox,
				MailOutboxLogEventType.PROCESSING_STARTED,
				fromStatus,
				outbox.getStatus()
			);
		}

		return dueOutboxes.stream()
			.map(MailOutbox::getId)
			.toList();
	}

	@Transactional
	public void deliver(Long outboxId, LocalDateTime now) {
		MailOutbox outbox = mailOutboxRepository.findById(outboxId)
			.orElse(null);
		if (outbox == null || outbox.getStatus() != MailOutboxStatus.PROCESSING) {
			return;
		}

		try {
			PreparedMailMessage preparedMail = resolveHandler(outbox.getMailType()).prepare(outbox.getPayloadJson());
			ujaxSmtpMailSender.send(outbox.getRecipientEmail(), preparedMail.subject(), preparedMail.content());
			MailOutboxStatus fromStatus = outbox.getStatus();
			outbox.markSent(now);
			mailOutboxLogRecorder.record(outbox, MailOutboxLogEventType.SENT, fromStatus, outbox.getStatus());
		} catch (RuntimeException exception) {
			handleFailure(outbox, now, exception);
		}
	}

	private void handleFailure(MailOutbox outbox, LocalDateTime now, RuntimeException exception) {
		String summarizedError = summarizeError(exception);
		MailOutboxStatus fromStatus = outbox.getStatus();
		if (outbox.isRetryExhausted(properties.maxAttempts())) {
			outbox.markFailed(summarizedError);
			mailOutboxLogRecorder.record(outbox, MailOutboxLogEventType.FAILED, fromStatus, outbox.getStatus());
			return;
		}
		outbox.scheduleRetry(now.plusMinutes(properties.retryDelayMinutes()), summarizedError);
		mailOutboxLogRecorder.record(outbox, MailOutboxLogEventType.RETRY_SCHEDULED, fromStatus, outbox.getStatus());
	}

	private MailOutboxHandler resolveHandler(MailType mailType) {
		return handlers.stream()
			.filter(handler -> handler.mailType() == mailType)
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("mail handler not found for type: " + mailType));
	}

	private String summarizeError(RuntimeException exception) {
		String message = exception.getMessage();
		if (message == null || message.isBlank()) {
			message = exception.getClass().getSimpleName();
		}
		if (message.length() <= MAX_ERROR_LENGTH) {
			return message;
		}
		return message.substring(0, MAX_ERROR_LENGTH);
	}
}
