package com.ujax.application.webhook;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ujax.domain.webhook.WebhookAlert;
import com.ujax.domain.webhook.WebhookAlertStatus;

@ExtendWith(OutputCaptureExtension.class)
class WebhookAlertEventLoggerTest {

	private final WebhookAlertEventLogger webhookAlertEventLogger = new WebhookAlertEventLogger();

	@Test
	@DisplayName("retry scheduled 이벤트를 structured log 로 남긴다")
	void logRetryScheduled(CapturedOutput output) {
		WebhookAlert alert = WebhookAlert.create(101L, 11L, LocalDateTime.of(2026, 4, 15, 10, 0));
		ReflectionTestUtils.setField(alert, "id", 7L);
		alert.markProcessing();
		alert.markRetry(LocalDateTime.of(2026, 4, 15, 10, 5), 5, "network timeout");

		webhookAlertEventLogger.logRetryScheduled(alert, WebhookAlertStatus.PROCESSING, LocalDateTime.of(2026, 4, 15, 10, 1));

		assertThat(output.getOut())
			.contains("event=webhook_alert")
			.contains("eventType=RETRY_SCHEDULED")
			.contains("alertId=7")
			.contains("lastError=network timeout")
			.contains("retryable=true");
	}

	@Test
	@DisplayName("failed 이벤트를 structured log 로 남긴다")
	void logFailed(CapturedOutput output) {
		WebhookAlert alert = WebhookAlert.create(101L, 11L, LocalDateTime.of(2026, 4, 15, 10, 0));
		ReflectionTestUtils.setField(alert, "id", 9L);
		alert.markProcessing();

		webhookAlertEventLogger.logFailed(alert, LocalDateTime.of(2026, 4, 15, 10, 2), "bad request", false);

		assertThat(output.getOut())
			.contains("event=webhook_alert")
			.contains("eventType=FAILED")
			.contains("alertId=9")
			.contains("lastError=bad request")
			.contains("retryable=false");
	}
}
