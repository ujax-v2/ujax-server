package com.ujax.application.mail.outbox;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ujax.application.mail.outbox.handler.MailOutboxHandler;
import com.ujax.application.mail.outbox.message.PreparedMailMessage;
import com.ujax.domain.mail.MailOutbox;
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
	private final MailSender mailSender;
	private final MailOutboxDeliveryProperties properties;
	private final MailOutboxEventLogger mailOutboxEventLogger;

	@Transactional
	public void recoverStuckProcessing(LocalDateTime now) {
		List<MailOutbox> stuckOutboxes = mailOutboxRepository.findAllByStatusAndUpdatedAtBefore(
			MailOutboxStatus.PROCESSING,
			now.minusMinutes(properties.stuckProcessingMinutes())
		);

		for (MailOutbox outbox : stuckOutboxes) {
			outbox.recoverToPending(now);
			mailOutboxEventLogger.logRecovered(outbox);
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
			mailOutboxEventLogger.logProcessingStarted(outbox, fromStatus);
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
			mailSender.send(outbox.getRecipientEmail(), preparedMail.subject(), preparedMail.content());
			recordSuccessAndDelete(outbox, now);
		} catch (RuntimeException exception) {
			handleFailure(outbox, now, exception);
		}
	}

	private void recordSuccessAndDelete(MailOutbox outbox, LocalDateTime now) {
		mailOutboxEventLogger.logSent(outbox, now);
		mailOutboxRepository.delete(outbox);
	}

	private void handleFailure(MailOutbox outbox, LocalDateTime now, RuntimeException exception) {
		String summarizedError = summarizeError(exception);
		MailOutboxStatus fromStatus = outbox.getStatus();
		if (outbox.isRetryExhausted(properties.maxAttempts())) {
			mailOutboxEventLogger.logFailed(outbox, summarizedError);
			mailOutboxRepository.delete(outbox);
			return;
		}
		outbox.scheduleRetry(now.plusMinutes(properties.retryDelayMinutes()), summarizedError);
		mailOutboxEventLogger.logRetryScheduled(outbox, fromStatus);
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
