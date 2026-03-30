package com.ujax.application.mail;

import java.io.UnsupportedEncodingException;

import jakarta.mail.MessagingException;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;

import com.ujax.global.exception.common.ExternalApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailDeliveryRetryExecutor {

	private static final String SMTP_SERVICE = "SMTP";

	private final MailDeliveryRetryProperties properties;

	public void execute(String failureMessage, MailDeliveryAction action) {
		for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
			try {
				action.run();
				return;
			} catch (MailException | MessagingException | UnsupportedEncodingException ex) {
				if (attempt >= properties.maxAttempts()) {
					throw new ExternalApiException(SMTP_SERVICE, failureMessage, ex);
				}

				log.warn(
					"SMTP send attempt {} of {} failed. Retrying in {} ms.",
					attempt,
					properties.maxAttempts(),
					properties.delayMillis(),
					ex
				);
				waitBeforeRetry(failureMessage, ex);
			}
		}

		throw new IllegalStateException("Mail retry attempts must be greater than 0.");
	}

	private void waitBeforeRetry(String failureMessage, Exception failureCause) {
		if (properties.delayMillis() <= 0) {
			return;
		}

		try {
			Thread.sleep(properties.delayMillis());
		} catch (InterruptedException interruptedException) {
			Thread.currentThread().interrupt();
			interruptedException.addSuppressed(failureCause);
			throw new ExternalApiException(SMTP_SERVICE, failureMessage, interruptedException);
		}
	}

	@FunctionalInterface
	public interface MailDeliveryAction {
		void run() throws MessagingException, UnsupportedEncodingException;
	}
}
